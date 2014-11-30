/*
 * Settings.java
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.log4j.Logger;

import com.smmsp.core.utils.OSAPI;

/**
 * Class to provide configuration items for the application
 * 
 * @author Sean
 *
 */
public enum Settings
{
	/**
	 * The username used to log into USGS EarthExplorer
	 */
	USERNAME("username", "USGS EarthExplorer Username",
			NonNullStringVerifier.INSTANCE),

	/**
	 * The password used to log into USGS EarthExplorer
	 */
	PASSWORD("password", "USGS EarthExplorer Password",
			NonNullStringVerifier.INSTANCE),

	/**
	 * Minimum latitude to attempt downloading
	 */
	MIN_LAT("minLatitude",
			"Minimum Latitude to Download [inclusive] Decimal Degrees WGS84",
			new IntRangeVerifier(-90, 90)),

	/**
	 * Minimum longitude to attempt downloading
	 */
	MIN_LON("minLongitude",
			"Minimum Longitude to Download [inclusive] Decimal Degrees WGS84",
			new IntRangeVerifier(-180, 180)),

	/**
	 * Maximum latitude to attempt downloading
	 */
	MAX_LAT("maxLatitude",
			"Maximum Latitude to Download [inclusive] Decimal Degrees WGS84",
			new IntRangeVerifier(-90, 90)),

	/**
	 * Maximum longitude to attempt downloading
	 */
	MAX_LON("maxLongitude",
			"Maximum Longitude to Download [inclusive] Decimal Degrees WGS84",
			new IntRangeVerifier(-180, 180)),

	/**
	 * The number of download threads to use.
	 */
	NUM_THREADS("numThreads",
			"Number of Download Threads to use [1, MAX_INT].",
			new IntRangeVerifier(1, Integer.MAX_VALUE)),

	/**
	 * The configuration file to use to override these defaults
	 */
	CONFIG_FILE("configFile", "Configuration file (defaults to "
			+ OSAPI.getCacheDirectory() + File.separator
			+ "settings.properties", NonNullStringVerifier.INSTANCE);

	/**
	 * Logger
	 */
	private static final Logger LOG = Logger.getLogger(Settings.class);

	/**
	 * Default configuration file path.
	 */
	private static final Path DEFAULT_CONFIG_FILE = Paths.get(OSAPI
			.getCacheDirectory().toString(), "settings.properties");

	/*
	 * Loaded at class load time.
	 */
	static
	{
		// try settings file first - may not exist
		loadFromSettingsFile();

		// Grab from system property overrides
		checkAllSettings(
				setting -> System.getProperty(setting.getName()) != null,
				setting -> setting.setValue(System.getProperty(setting
						.getName())));

		// lock them back into the file.
		if (checkAllSettings(setting -> setting.verify(), null))
		{
			saveToFile();
		}
	}

	/**
	 * The name of the configuration item
	 */
	private final String name;

	/**
	 * The stringified value of the item.
	 */
	private String value;

	/**
	 * The description of the item (for printing)
	 */
	private final String description;

	/**
	 * How do we verify the contents of the item?
	 */
	private final Verifier verifier;

	/**
	 * Private constructor for settings
	 * 
	 * @param name The name of the setting
	 * @param description The description
	 * @param v The verifier to use
	 */
	private Settings(final String name, final String description,
			final Verifier v)
	{
		this.name = name;
		this.description = description;
		this.verifier = v;
	}

	/**
	 * @return The name
	 * @see #name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @return The description
	 * @see #description
	 */
	public String getDescripton()
	{
		return description;
	}

	/**
	 * @return The value
	 * @see #value
	 */
	public String getValue()
	{
		return value;
	}

	/**
	 * Sets the value
	 * 
	 * @see #value
	 * @param v the value to set
	 */
	public void setValue(String v)
	{
		this.value = v;
	}

	/**
	 * @return The integer representation of a particular value
	 * @throws NumberFormatException when value isn't an integer
	 * @see #value
	 */
	public int getIntValue()
	{
		return Integer.valueOf(value);
	}

	/**
	 * @return Uses the Setting's verifier to verify it's value
	 */
	public boolean verify()
	{
		return verifier.verify(getValue());
	}

	/**
	 * Loads all the settings from the file (if it exists)
	 */
	private static void loadFromSettingsFile()
	{
		try
		{
			Path path = null;
			boolean useDefault = true;
			if (CONFIG_FILE.verify())
			{
				path = Paths.get(CONFIG_FILE.getValue());
				useDefault = Files.exists(path);
			}

			if (useDefault)
			{
				path = DEFAULT_CONFIG_FILE;
				OSAPI.ensureCacheDirExists();
			}

			if (path == null || !Files.exists(path))
			{
				return;
			}
			Properties p = new Properties();

			try (FileInputStream fis = new FileInputStream(path.toFile()))
			{
				p.load(fis);
			}

			consumeAllSettings(setting -> setting.setValue((String) p
					.get(setting.getName())));

		} catch (IOException e)
		{
			LOG.error("Unexpected exception.", e);
		}
	}

	/**
	 * Saves all the individual settings to the file specified by CONFIG_FILE
	 */
	protected static void saveToFile()
	{
		try
		{
			Path path = null;
			boolean useDefault = true;
			if (CONFIG_FILE.verify())
			{
				path = Paths.get(CONFIG_FILE.getValue());
				useDefault = Files.exists(path);
			}

			if (useDefault)
			{
				path = DEFAULT_CONFIG_FILE;
				OSAPI.ensureCacheDirExists();
			}

			if (path == null)
			{
				return;
			}
			final Properties p = new Properties();

			Arrays.asList(values())
					.stream()
					.filter(setting -> setting != CONFIG_FILE)
					.forEach(
							setting -> p.setProperty(setting.getName(),
									setting.getValue()));

			try (FileOutputStream fos = new FileOutputStream(path.toFile()))
			{
				p.store(fos, "");
			}

		} catch (final IOException e)
		{
			LOG.error("Unexpected error", e);
		}
	}

	/**
	 * Walks through all Settings elements and provides them to the consumer
	 * (intended for Lambas and the whatnot)
	 * 
	 * @param o Consumer to accept each individual settings
	 */
	public static void consumeAllSettings(Consumer<Settings> o)
	{
		for (Settings settings : values())
		{
			o.accept(settings);
		}
	}

	/**
	 * Check all settings against a certain predicate.
	 * 
	 * @param p The predicate to check against
	 * @param failer The Consumer to call when the predicate is true.
	 * @return True if all settings pass the predicate
	 */
	public static boolean checkAllSettings(Predicate<Settings> p,
			Consumer<Settings> truth)
	{
		boolean good = true;
		for (Settings setting : values())
		{
			if (p.test(setting))
			{
				if (setting != CONFIG_FILE)
				{
					good = false;
				}

				if (truth != null)
				{
					truth.accept(setting);
				}
			}
		}

		return good;
	}

	/**
	 * Interface used as a validation tool for individual settings.
	 * 
	 * @author Sean
	 */
	private interface Verifier
	{
		public boolean verify(String value);
	}

	/**
	 * Validates that the value is a number between a particular range
	 * [inclusive, inclusive]
	 * 
	 * @author Sean
	 */
	protected static class IntRangeVerifier implements Verifier
	{
		private final int minValueInclusive;
		private final int maxValueInclusive;

		public IntRangeVerifier(int minValueInclusive, int maxValueInclusive)
		{
			this.minValueInclusive = minValueInclusive;
			this.maxValueInclusive = maxValueInclusive;
		}

		@Override
		public boolean verify(String value)
		{
			try
			{
				int intVal = Integer.valueOf(value);
				if (intVal >= minValueInclusive && intVal <= maxValueInclusive)
				{
					return true;
				}
			} catch (final Exception e)
			{
				// no-op
			}

			return false;
		}
	}

	/**
	 * Ensures that the value passed in is not null.
	 * 
	 * @author Sean
	 */
	protected static class NonNullStringVerifier implements Verifier
	{
		public static NonNullStringVerifier INSTANCE = new NonNullStringVerifier();

		@Override
		public boolean verify(String value)
		{
			return value != null;
		}
	}

}
