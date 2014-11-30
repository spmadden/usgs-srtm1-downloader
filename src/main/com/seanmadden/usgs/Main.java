/*
 * Main.java
 * 
 * Copyright (C) 2014 Sean P Madden
 * 
 * The MIT License (MIT)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 */
package com.seanmadden.usgs;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.smmsp.core.net.HTTPConnection;
import com.smmsp.core.net.HTTPConnection.RequestMethod;
import com.smmsp.core.net.HTTPResponse;

/**
 * Primary entry point into the program. Downloads a series of SRTM1 DTED data
 * based on the Settings.
 * 
 * @author Sean
 *
 */
public class Main
{

	/**
	 * Logger
	 */
	protected static final Logger LOG = Logger.getLogger(Main.class);

	/**
	 * Download URL for the Earth Explorer
	 */
	protected static final String SRTM1_DOWNLOAD_URL = "http://earthexplorer.usgs.gov/download/8360/";

	/**
	 * Login URL for the Earth Explorer
	 */
	protected static final String LOGIN_URL = "https://earthexplorer.usgs.gov/login";

	/**
	 * Rentrant Lock for logins
	 */
	protected static final ReentrantReadWriteLock LOGIN_LOCK = new ReentrantReadWriteLock();

	/**
	 * Series of cookies grabbed from the Login.
	 */
	protected static Map<String, String> LOGIN_COOKIES = new HashMap<>();

	/**
	 * Thread Pool for concurrent downloads
	 */
	protected static ExecutorService THREAD_POOL;

	public static void main(String[] args)
	{
		// Default to the INFO level.
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);

		if (!verifyProperties(System.err))
		{
			System.out.println();
			printUsage(System.out);
			return;
		}

		if (args.length != 0)
		{
			printUsage(System.out);
			return;
		}

		THREAD_POOL = Executors.newFixedThreadPool(
				Settings.NUM_THREADS.getIntValue(), new ThreadFactory()
				{
					int num = 0;

					@Override
					public Thread newThread(Runnable r)
					{
						Thread t = new Thread(r, "Downloader Thread " + num++);
						t.setDaemon(true);
						return t;
					}
				});

		downloadAllFilesThreaded();

		THREAD_POOL.shutdown();
	}

	/**
	 * Prints usage upon error.
	 * 
	 * @param out Where to print the useage
	 */
	private static void printUsage(PrintStream out)
	{
		out.println("Usage Instructions for USGS SRTM1 Downloader:");
		out.println("\tjava {options} -jar usgs-srtm1-downloader.jar");
		out.println();
		out.println("Options:");
		Settings.consumeAllSettings(setting ->
		{
			out.println("\t -D" + setting.getName() + "={"
					+ setting.getDescripton() + "}");
		});
		out.println();
		out.println("Note:  You will need a USGS Earth Explorer Account.  Register at earthexplorer.usgs.gov");

	}

	/**
	 * Verifies that all properties are valid, printing each that is not.
	 * 
	 * @param out The PrintStream to print errors to
	 * @return True if all settings pass validation
	 */
	private static boolean verifyProperties(PrintStream out)
	{
		return Settings
				.checkAllSettings(
						setting -> !setting.verify()
								&& setting != Settings.CONFIG_FILE,
						setting -> out.println("Missing or invalid value for: "
								+ setting.getName()));
	}

	/**
	 * Download all files as described by Settings in a multi-threaded manner
	 */
	private static void downloadAllFilesThreaded()
	{
		int minLat = Settings.MIN_LAT.getIntValue();
		int maxLat = Settings.MAX_LAT.getIntValue();
		int minLon = Settings.MIN_LON.getIntValue();
		int maxLon = Settings.MAX_LON.getIntValue();

		int startLat = Math.min(minLat, maxLat);
		int endLat = Math.max(minLat, maxLat);
		int startLon = Math.min(minLon, maxLon);
		int endLon = Math.max(minLon, maxLon);

		List<Future<Void>> futures = new LinkedList<>();

		for (int lat = startLat; lat <= endLat; ++lat)
		{
			for (int lon = startLon; lon <= endLon; ++lon)
			{
				futures.add(THREAD_POOL.submit(new SingleDownloaderCallable(
						lat, lon)));
			}
		}

		futures.forEach(future ->
		{
			try
			{
				future.get();
			} catch (InterruptedException | ExecutionException e)
			{
				LOG.error("Unexpected error downloading file", e);
			}
		});
	}

	/**
	 * 0. Does a double-checking synchronize against the Login lock to ensure
	 * that we're the only thread waiting to LOGIN 1. Makes an initial request
	 * to the /login to grab the PHPSESSID cookie 2. Makes a POST to the /login
	 * with the username + password + cookies to get the last session cookies 3.
	 * Saves the Session cookies in {@link #LOGIN_COOKIES}
	 */
	private static void getLoginCookies()
	{

		if (!LOGIN_LOCK.isWriteLocked())
		{
			synchronized (LOGIN_LOCK)
			{
				if (!LOGIN_LOCK.isWriteLocked())
				{
					LOGIN_LOCK.writeLock().lock();
				}
			}
		}
		if (!LOGIN_LOCK.isWriteLockedByCurrentThread())
		{
			return;
		}
		try
		{
			LOG.info("Attempting login...");
			HTTPConnection conn = new HTTPConnection(LOGIN_URL);

			Map<String, String> cookies = grabAllCookiesFromRequest(conn);

			conn = new HTTPConnection(LOGIN_URL, RequestMethod.POST);
			conn.addFormField("username", Settings.USERNAME.getValue());
			conn.addFormField("password", Settings.PASSWORD.getValue());
			conn.addFormField("rememberMe", "1");
			conn.addFormField("submit", "");

			for (String value : cookies.keySet())
			{
				LOGIN_COOKIES.put(value, cookies.get(value));
			}
			addCookies(conn);

			cookies = grabAllCookiesFromRequest(conn);
			for (String value : cookies.keySet())
			{
				LOGIN_COOKIES.put(value, cookies.get(value));
			}

		} catch (MalformedURLException e)
		{
			e.printStackTrace();
		} finally
		{
			LOGIN_LOCK.writeLock().unlock();
		}

	}

	/**
	 * Pulls all of the cookies in the request and returns them in a Key:Value
	 * pairing
	 * 
	 * @param conn The connection to pull cookies form
	 * @return A Key:Value map for all cookie values
	 */
	private static Map<String, String> grabAllCookiesFromRequest(
			HTTPConnection conn)
	{
		HTTPResponse response = conn.getResponse();

		Map<String, List<String>> headers = response.getResponseHeaders();

		if (!headers.containsKey("Set-Cookie"))
		{
			return null;
		}

		List<String> cookies = headers.get("Set-Cookie");
		Map<String, String> parsedCookies = new HashMap<>();
		for (String cookie : cookies)
		{
			List<HttpCookie> parse = HttpCookie.parse(cookie);
			for (HttpCookie httpCookie : parse)
			{
				parsedCookies.put(httpCookie.getName(), httpCookie.getValue());
			}

		}
		return parsedCookies;
	}

	/**
	 * Creates the full Download URL for a particular Latitude and Longitude
	 * 
	 * @param lat Decimal Degrees Latitude WGS84
	 * @param lon Decimal Degrees Longitude WGS84
	 * @return the full URL for a particular lat and long
	 */
	private static String makeFileName(int lat, int lon)
	{
		// SRTM1N11W009V3
		final char NS = (lat >= 0) ? 'N' : 'S';
		final char EW = (lon >= 0) ? 'E' : 'W';

		DecimalFormat TWO = new DecimalFormat("00");
		DecimalFormat THREE = new DecimalFormat("000");

		StringBuilder bld = new StringBuilder();
		bld.append(SRTM1_DOWNLOAD_URL);
		bld.append("SRTM1");
		bld.append(NS);
		bld.append(TWO.format(Math.abs(lat)));
		bld.append(EW);
		bld.append(THREE.format(Math.abs(lon)));
		bld.append("V3");
		bld.append("/DTED/EE");

		return bld.toString();
	}

	/**
	 * Does all the hard work.
	 * 
	 * - Makes an initial request for the file - Follows all Header redirects
	 * until we get to the file (*.dt2) -- If a redirect passes you to /login
	 * again, attempts login. - Downloads that file to the current directory.
	 * 
	 * @param lat
	 * @param lon
	 */
	protected static void downloadSingleFile(int lat, int lon)
	{
		String URL = makeFileName(lat, lon);
		LOG.debug("Attempting download of file: " + URL);
		try
		{
			HTTPConnection conn = new HTTPConnection(URL);
			while (true)
			{
				Lock readLock = LOGIN_LOCK.readLock();
				readLock.lock();
				try
				{
					addCookies(conn);

					String fileName = null;

					long startTime = System.currentTimeMillis();
					if (URL.contains(".dt2"))
					{
						fileName = URL.substring(URL.lastIndexOf('/') + 1,
								URL.indexOf(".dt2") + 4);

						LOG.info("Starting download of " + fileName);

					}

					HTTPResponse response = conn.getResponse();
					LOG.debug("Got response code: "
							+ response.getResponseCode());

					Map<String, List<String>> headers = response
							.getResponseHeaders();
					if (headers.containsKey("Location"))
					{
						URL = headers.get("Location").get(0);

						if (URL.startsWith("/login"))
						{
							if (!LOGIN_LOCK.isWriteLocked())
							{
								readLock.unlock();
								getLoginCookies();
								readLock.lock();
							}
							continue;
						}

						LOG.debug("Redirecting to: " + URL);
						conn = new HTTPConnection(URL);
						continue;
					}

					if (URL.contains(".dt2"))
					{
						long numBytes = Files.copy(response.getStream(),
								new File(fileName).toPath(),
								StandardCopyOption.REPLACE_EXISTING);

						long deltaMillis = System.currentTimeMillis()
								- startTime;
						double avgSpeed = (numBytes / 1024.)
								/ (deltaMillis / 1000.);
						LOG.info("Finished download of " + fileName
								+ " avg speed of " + avgSpeed + " kb/s");
					}
				} finally
				{
					readLock.unlock();
				}

				break;
			}

		} catch (IOException e)
		{
			LOG.error(e, e);
		}
	}

	/**
	 * Adds all the Login Cookies to the HTTP connection specified by CONN
	 * 
	 * @param conn
	 */
	private static void addCookies(HTTPConnection conn)
	{
		StringBuffer buf = new StringBuffer();
		Iterator<String> rator = LOGIN_COOKIES.keySet().iterator();
		while (rator.hasNext())
		{
			String key = rator.next();
			buf.append(key);
			buf.append("=");
			buf.append(LOGIN_COOKIES.get(key));
			if (rator.hasNext())
			{
				buf.append("; ");
			}
		}

		conn.addHeader("Cookie", buf.toString());
	}

	/**
	 * Calls downloadSingleFile in a concurrent manner.
	 * 
	 * @author Sean
	 *
	 */
	protected static class SingleDownloaderCallable implements Callable<Void>
	{
		private int lat;
		private int lon;

		public SingleDownloaderCallable(int lat, int lon)
		{
			this.lat = lat;
			this.lon = lon;
		}

		@Override
		public Void call()
		{
			downloadSingleFile(lat, lon);
			return null;
		}

	}
}
