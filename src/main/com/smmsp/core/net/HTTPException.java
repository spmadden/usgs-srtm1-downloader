/*
 * HTTPException.java
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
package com.smmsp.core.net;

/**
 * @author sean
 *
 */
public class HTTPException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2644142297641108465L;

	/**
	 * @param arg0
	 * @param arg1
	 */
	public HTTPException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	/**
	 * @param arg0
	 */
	public HTTPException(String arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public HTTPException(Throwable arg0) {
		super(arg0);
	}

}
