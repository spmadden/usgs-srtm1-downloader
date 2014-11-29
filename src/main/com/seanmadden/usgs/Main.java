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
import java.io.InputStream;
import java.io.PrintStream;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLSocketFactory;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.smmsp.core.net.HTTPConnection;
import com.smmsp.core.net.HTTPConnection.RequestMethod;
import com.smmsp.core.net.HTTPResponse;

/**
 * Primary entry point into the program.
 * 
 * @author Sean
 *
 */
public class Main
{
	
	protected static final Logger LOG = Logger.getLogger(Main.class);

	protected static final String SRTM1_DOWNLOAD_URL = "http://earthexplorer.usgs.gov/download/8360/";

	protected static final String LOGIN_URL = "https://earthexplorer.usgs.gov/login";

	protected static final AtomicBoolean WAIT_FOR_LOGIN = new AtomicBoolean(
			false);

	protected static Map<String, String> LOGIN_COOKIES = new HashMap<>();

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		BasicConfigurator.configure();

		if (!verifyProperties(System.out))
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
		ExecutorService threadPool = Executors.newFixedThreadPool(
				Settings.NUM_THREADS.getIntValue(), new ThreadFactory()
				{
					int num = 0;

					@Override
					public Thread newThread(Runnable r)
					{
						return new Thread(r, "Downloader Thread " + num++);
					}
				});

		getLoginCookies();
		downloadAllFiles();
	}

	private static void printUsage(PrintStream out)
	{
		out.println("Usage Instructions for USGS SRTM1 Downloader:");
		out.println("\tjava {options} -jar usgs-srtm1-downloader.jar");
		out.println();
		out.println("Options:");
		for (Settings setting : Settings.values())
		{
			out.println("\t -D" + setting.getName() + "={"
					+ setting.getDescripton() + "}");
		}
		out.println();
		out.println("Note:  You will need a USGS Earth Explorer Account.  Register at earthexplorer.usgs.gov");

	}

	private static boolean verifyProperties(PrintStream out)
	{
		boolean good = true;

		for (Settings setting : Settings.values())
		{
			if (!setting.verify())
			{
				good = false;
				out.println("Missing or invalid value for: "
						+ setting.getName());
			}
		}

		return good;
	}

	private static void downloadAllFiles()
	{
		int minLat = Settings.MIN_LAT.getIntValue();
		int maxLat = Settings.MAX_LAT.getIntValue();
		int minLon = Settings.MIN_LON.getIntValue();
		int maxLon = Settings.MAX_LON.getIntValue();

		for (int lat = minLat; lat <= maxLat; ++lat)
		{
			for (int lon = minLon; lon <= maxLon; ++lon)
			{
				downloadSingleFile(lat, lon);
			}
		}
	}

	private static void getLoginCookies()
	{
		WAIT_FOR_LOGIN.set(true);

		try
		{
			HTTPConnection conn = new HTTPConnection(LOGIN_URL);

			Map<String, String> cookies = grabAllCookiesFromRequest(conn);
			System.out.println(cookies);

			conn = new HTTPConnection(LOGIN_URL, RequestMethod.POST);
			conn.addFormField("username", Settings.USERNAME.getValue());
			conn.addFormField("password", Settings.PASSWORD.getValue());
			conn.addFormField("rememberMe", "1");
			conn.addFormField("submit", "");

			for (String value : cookies.keySet())
			{
				LOGIN_COOKIES.put(value, cookies.get(value));
				conn.addHeader("Cookie", cookies.get(value));
			}

			cookies = grabAllCookiesFromRequest(conn);
			for (String value : cookies.keySet())
			{
				LOGIN_COOKIES.put(value, cookies.get(value));
			}

		} catch (MalformedURLException e)
		{
			e.printStackTrace();
		}

		WAIT_FOR_LOGIN.set(false);
		synchronized(WAIT_FOR_LOGIN)
		{
			WAIT_FOR_LOGIN.notifyAll();
		}

	}

	private static Map<String, String> grabAllCookiesFromRequest(HTTPConnection conn)
	{
		HTTPResponse response = conn.getResponse();
		
		Map<String, List<String>> headers = response.getResponseHeaders();
		
		if(!headers.containsKey("Set-Cookie"))
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

	private static void downloadSingleFile(int lat, int lon)
	{
		String URL = makeFileName(lat, lon);
		LOG.debug("Attempting download of file: " + URL);
		try
		{
			while (WAIT_FOR_LOGIN.get())
			{
				try
				{
					WAIT_FOR_LOGIN.wait();
				} catch (InterruptedException e)
				{
					// no-op
				}
			}

			HTTPConnection conn = new HTTPConnection(URL);
			while(true)
			{
				addCookies(conn);
				HTTPResponse response = conn.getResponse();
				
				LOG.debug("Got response code: "+ response.getResponseCode());
				Map<String, List<String>> headers = response.getResponseHeaders();
				if(headers.containsKey("Location"))
				{
					URL = headers.get("Location").get(0);
					LOG.debug("Redirecting to: " + URL);
					conn = new HTTPConnection(URL);
					continue;
				}
				
				if(URL.contains(".dt2"))
				{
					String fileName = URL.substring( URL.lastIndexOf('/')+1, URL.indexOf(".dt2") +4);
					
					Files.copy(response.getStream(), 
							new File(fileName).toPath(), 
							StandardCopyOption.REPLACE_EXISTING);
				}
				
				
				break;
			}


		} catch (IOException e)
		{
			LOG.error(e, e);
		}
	}
	
	private static void addCookies(HTTPConnection conn)
	{
		StringBuffer buf = new StringBuffer();
		Iterator<String> rator = LOGIN_COOKIES.keySet().iterator();
		while(rator.hasNext())
		{
			String key = rator.next();
			buf.append(key);
			buf.append("=");
			buf.append(LOGIN_COOKIES.get(key));
			if(rator.hasNext())
			{
				buf.append("; ");
			}
		}
		
		conn.addHeader("Cookie", buf.toString());
	}
}
