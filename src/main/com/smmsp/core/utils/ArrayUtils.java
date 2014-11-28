/*
 * ArrayUtils.java
 * 
 * Copyright (C) 2012 Sean P Madden
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

/**
 * @author sean
 *
 */
public final class ArrayUtils {
	
	/**
	 * Empty constructor.
	 */
	private ArrayUtils(){
		// do nothing.
	}

	public static int[] combine(final int[]... arrays){
		int[] combined = new int[0];
		for(int[] arr : arrays){
			combined = combine(combined, arr);
		}
		return combined;
	}
	
	public static int[] combine(final int[] first, final int[] second){
		final int[] combined = new int[first.length + second.length];
		int position = 0;
		for(int i = 0; i < first.length; ++i){
			combined[position++] = first[i];
		}
		for(int i = 0; i < second.length; ++i){
			combined[position++] = second[i];
		}
		return combined;
	}
	
	public static byte[] combine(final byte[]... arrays){
		byte[] combined = new byte[0];
		for(byte[] arr : arrays){
			combined = combine(combined, arr);
		}
		return combined;
	}
	
	public static byte[] combine(final byte[] first, final byte[] second){
		final byte[] combined = new byte[first.length + second.length];
		int position = 0;
		for(int i = 0; i < first.length; ++i){
			combined[position++] = first[i];
		}
		for(int i = 0; i < second.length; ++i){
			combined[position++] = second[i];
		}
		return combined;
	}
	
	
}
