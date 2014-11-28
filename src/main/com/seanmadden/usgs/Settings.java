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
	USERNAME("username", "USGS EarthExplorer Username"),
	PASSWORD("password", "USGS EarthExplorer Password"),
	MIN_LAT("minLatitude", "Minimum Latitude to Download [inclusive] Decimal Degrees WGS84"),
	MIN_LON("minLongitude", "Minimum Longitude to Download [inclusive] Decimal Degrees WGS84"),
	MAX_LAT("maxLatitude", "Maximum Latitude to Download [inclusive] Decimal Degrees WGS84"),
	MAX_LON("maxLongitude", "Maximum Longitude to Download [inclusive] Decimal Degrees WGS84")
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
	
	private Settings(String name, String description)
	{
		this.name = name;
		this.description = description;
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
	
	
}
