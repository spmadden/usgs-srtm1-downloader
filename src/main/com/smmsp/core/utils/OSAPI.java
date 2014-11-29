/*
 * OSAPI.java
 * 
 * Copyright (C) 2013 Sean P Madden
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * If you would like to license this code under the GNU LGPL, please
 * see http://www.seanmadden.net/licensing for details.
 */
package com.smmsp.core.utils;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This class provides an abstraction to make the OS transparent to
 * the application.  Things like cache & home directories live here.
 * @author sean
 *
 */
public final class OSAPI {

	/**
	 * The directory what we're going to store our files in
	 */
	public final static String CACHE_DIR_NAME = ".usgs-downloader";
	
	public final static String OS_NAME = System.getProperty("os.name");
	
	public final static String OS_VER = System.getProperty("os.version");
	
	public final static String OS_ARCH = System.getProperty("os.arch");
	
	/**
	 * Internal representation of the operating system (as an 
	 * enumeration to limit the number of options)
	 * @author sean
	 *
	 */
	public static enum OSAPI_Internal{
		
		WINDOWS_8{
			/**
			 * The same as Windows 7
			 */
			public Path getCacheDirectory(){
				return WINDOWS_7.getCacheDirectory();
			};
			
			public boolean isThisOS(){
				return "Windows 8".equals(OS_NAME) 
						&& OS_VER.charAt(0) == '7';
			}
		},
		
		WINDOWS_7{
			public Path getCacheDirectory(){
				final String homeDirectory = System.getProperty("user.home");
				
				return FileSystems.getDefault().getPath(
						homeDirectory,
						"AppData",
						"Roaming",
						CACHE_DIR_NAME);
			}
			
			public boolean isThisOS(){
				return "Windows 7".equals(OS_NAME)
						&& "6.1".equals(OS_VER);
			}
		},
		
		WINDOWS_VISTA{
			/**
			 * Same as windows 7
			 */
			public Path getCacheDirectory(){
				return WINDOWS_7.getCacheDirectory();
			}
			
			public boolean isThisOS(){
				return "Windows Vista".equals(OS_NAME) 
						&& OS_VER.charAt(0) == '6';
			}
		},
		
		WINDOWS_XP{
			public Path getCacheDirectory(){
				final String homeDir = System.getProperty("user.home");
				
				return FileSystems.getDefault().getPath(
						homeDir,
						"Local Settings",
						"Application Data",
						CACHE_DIR_NAME
						);
			}
			
			public boolean isThisOS(){
				return "Windows XP".equals(OS_NAME) 
						&& OS_VER.charAt(0) == '5';
			}
		},
		
		//TODO write this.
		//MAC_OSX,
		
		//TODO write this.
		//ANDROID,
		
		LINUX{
			public Path getCacheDirectory(){
				final String homeDirectory = System.getProperty("user.home");
				return FileSystems.getDefault().getPath(
						homeDirectory, 
						CACHE_DIR_NAME
						);
			}
			
			/**
			 * Assume that this is the OS if none of the others have
			 * matched to date (there are so many variants we can't 
			 * hope to check them all).  Also we need a default if
			 * we can't detect the OS and linux should be fairly standard.
			 */
			public boolean isThisOS(){
				return true;
			}
		};
		
		/**
		 * Return the SMMSP cache directory for the current OS
		 * @return
		 */
		public abstract Path getCacheDirectory();
		
		/**
		 * Is this OS the one we're running on?
		 * @return
		 */
		public abstract boolean isThisOS();
			
	}
	
	/*
	 * On classloader load, determine the OS and load the internal
	 * variable.
	 */
	static {
		for(OSAPI_Internal inern : OSAPI_Internal.values()){
			if(inern.isThisOS()){
				internal = inern;
				break;
			}
		}
	}
	
	private static OSAPI_Internal internal;
	
	/**
	 * Empty constructor.
	 */
	private OSAPI(){
		// do nothing.
	}
	
	/**
	 * Returns the cache directory for SMMSP operations.
	 * @return
	 */
	public static Path getCacheDirectory(){
		return internal.getCacheDirectory();
	}
	
	/**
	 * Returns the name of this OS
	 * @return
	 */
	public static String getOSName(){
		return internal.toString();
	}
	
	/**
	 * Ensures that the cache directory exists, if it doesn't, then
	 * make it.
	 * @throws IOException
	 */
	public static void ensureCacheDirExists() throws IOException{
		final Path cacheDir = internal.getCacheDirectory();
		if(Files.notExists(cacheDir)){
			Files.createDirectory(cacheDir);
		}
	}
}
