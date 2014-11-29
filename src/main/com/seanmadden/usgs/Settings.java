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



/**
 * @author Sean
 *
 */
public enum Settings
{
	USERNAME("username", "USGS EarthExplorer Username", new NonNullStringVerifier()),
	PASSWORD("password", "USGS EarthExplorer Password", new NonNullStringVerifier()),
	MIN_LAT("minLatitude", "Minimum Latitude to Download [inclusive] Decimal Degrees WGS84", new IntRangeVerifier(-90, 90)),
	MIN_LON("minLongitude", "Minimum Longitude to Download [inclusive] Decimal Degrees WGS84", new IntRangeVerifier(-180, 180)),
	MAX_LAT("maxLatitude", "Maximum Latitude to Download [inclusive] Decimal Degrees WGS84", new IntRangeVerifier(-90, 90)),
	MAX_LON("maxLongitude", "Maximum Longitude to Download [inclusive] Decimal Degrees WGS84",new IntRangeVerifier(-180, 180)),
	NUM_THREADS("numThreads", "Number of Download Threads to use [1, MAX_INT].", new IntRangeVerifier(1, Integer.MAX_VALUE))
	;
	
	static
	{
		for (Settings setting : values())
		{
			setting.setValue(System.getProperty(setting.getName()));
		}
	}
	
	private final String name;
	private String value;
	private final String description;
	private final Verifier verifier;
	
	private Settings(String name, String description, Verifier v)
	{
		this.name = name;
		this.description = description;
		this.verifier = v;
	}
	
	public String getName()
	{
		return name;
	}
	public String getDescripton()
	{
		return description;
	}
	
	public String getValue()
	{
		return value;
	}
	
	public void setValue(String v)
	{
		this.value = v;
	}
	
	public int getIntValue()
	{
		return Integer.valueOf(value);
	}
	
	public boolean verify()
	{
		return verifier.verify(getValue());
	}
	
	private interface Verifier
	{
		public boolean verify(String value);
	}
	
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
				if(intVal >= minValueInclusive && intVal <= maxValueInclusive)
				{
					return true;
				}
			}
			catch(final Exception e)
			{
				// no-op
			}
			
			return false;
		}
	}
	
	protected static class NonNullStringVerifier implements Verifier
	{

		@Override
		public boolean verify(String value)
		{
			return value != null;
		}
		
	}
	
}
