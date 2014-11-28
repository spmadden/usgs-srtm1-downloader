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

import java.io.PrintStream;

/**
 * Primary entry point into the program.
 * @author Sean
 *
 */
public class Main
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		if(!verifyProperties(System.out))
		{
			printUsage(System.out);
			return;
		}
		
		if(args.length != 0)
		{
			printUsage(System.out);
			return;
		}
		
		
	}
	
	private static void printUsage(PrintStream out)
	{
		out.println("Usage Instructions for USGS SRTM1 Downloader:");
		out.println("\tjava -jar usgs-srtm1-downloader.jar {options}");
		out.println();
		out.println("Options:");
		for (Settings setting : Settings.values())
		{
			out.println("\t -D" + setting.getName() + "={" + setting.getDescripton()+"}");
		}
		out.println();
		out.println("Note:  You will need a USGS Earth Explorer Account.  Register at earthexplorer.usgs.gov");
		
	}
	
	private static void printProperties(PrintStream out)
	{
		for(Settings setting : Settings.values())
		{
			out.println(setting.getName() + " :: " + setting.getValue());
		}
	}
	
	private static boolean verifyProperties(PrintStream out)
	{
		boolean good = true;
		
		for (Settings setting : Settings.values())
		{
			if(setting.getValue() == null || setting.getValue().length() == 0)
			{
				good = false;
				out.println("Missing value for: " + setting.getName());
			}
		}
		
		return good;
	}

}
