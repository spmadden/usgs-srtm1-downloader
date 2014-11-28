/*
* HTTPConnection.java
* 
*    Copyright (C) 2012 Sean P Madden
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*
*    If you would like to license this code under the GNU LGPL, please see
*    http://www.seanmadden.net/licensing for details.
*
*/
package com.smmsp.core.net;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap; 
import java.util.Map;

import org.apache.log4j.Logger;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

/**
 * This class wraps the URL connection to provide an easy wrapper to
 * simply make HTTP Requests and return the data.
 *
 * @author Sean P Madden
 */
public class HTTPConnection extends AbstractInternetConnection {
	
	
	private static final Logger LOG = Logger.getLogger(HTTPConnection.class);

	/**
	 * All possible request methods.
	 * @author sean
	 *
	 */
	public static enum RequestMethod{
		GET,
		POST,
		PUT;
	}
	
	/**
	 * The URL to retrieve
	 */
	private final URL url;
	
	/**
	 * How to retrieve it.
	 */
	private final RequestMethod method;
	
	/**
	 * A list of headers.
	 */
	private final LinkedHashMap<String, String> headers;
	
	/**
	 * A list of form elements to be x-form-urlencoded.
	 */
	private final LinkedHashMap<String, String> formElems;
	
	/**
	 * Constructor
	 * @param url
	 * @throws MalformedURLException
	 */
	public HTTPConnection(final String u) 
					throws MalformedURLException{
		this(u, RequestMethod.GET);
	}
	
	/**
	 * Constructor.
	 * @param u
	 * @param method
	 * @throws MalformedURLException
	 */
	public HTTPConnection(
			final String u, 
			final RequestMethod method)
					throws MalformedURLException{
		url = new URL(u);
		this.method = method;
		this.formElems = new LinkedHashMap<>();
		this.headers = new LinkedHashMap<>();
	}
	
	/**
	 * Copy Constructor!
	 * @param other
	 */
	public HTTPConnection(final HTTPConnection other){
		this.formElems = new LinkedHashMap<>(other.formElems);
		this.headers = new LinkedHashMap<>(other.headers);
		this.url = other.url;
		this.method = other.method;
	}
	
	/**
	 * Adds a header to this request.
	 * @param key
	 * @param val
	 */
	public void addHeader(final String key, final String val){
		this.headers.put(key, val);
	}
	
	/**
	 * Adds a form field to this request.
	 * @param key
	 * @param val
	 */
	public void addFormField(final String key, final String val){
		this.formElems.put(key, val);
	}
	
	protected Socket openConnection() throws HTTPException, IOException{
		final String protocol = url.getProtocol();
		final Socket sock;
		int port = 80;
		if("http".equals(protocol) || "HTTP".equals(protocol)){
			sock = getSocket();
		}else if("https".equals(protocol) || "HTTPS".equals(protocol)){
			sock = getSSLSocket();
			port = 443;
		}else{
			throw new HTTPException("Protocol is not HTTP or HTTPS!");
		}
		
		SocketAddress addr = new InetSocketAddress(url.getHost(), port);
		sock.connect(addr, 10000); // 10s in millis.
		
		return sock;
	}
	
	protected static Socket getSSLSocket() throws IOException{
		return SSLSocketFactory.getDefault().createSocket();
	}
	
	protected static Socket getSocket() throws IOException{
		return SocketFactory.getDefault().createSocket();
	}
	
	/* (non-Javadoc)
	 * @see com.smmsp.core.net.InternetConnection#getDataStream()
	 */
	public InputStream getDataStream(){
		if(url == null){
			return null;
		}
		
		try (final Socket sock = openConnection())
		{
			final StringBuffer request = new StringBuffer();
			request.append(method.toString());
			request.append(' ');
			request.append(url.getPath());
			request.append(' ');
			request.append("HTTP/1.1");
			request.append("\r\n");
			
			addHeader("Host", url.getHost());
			addHeader("Accept", "*/*");
			addHeader("User-Agent", "SPM-HttpClient v. 1 Beta");
			
			final String body = generateBody();
			if(!"".equals(body)){
				LOG.debug(body);
				addHeader("Content-Type", "application/x-www-form-urlencoded");
			}
			
			
			headers.put("Content-Length", ""+ body.length());
			
			for(Map.Entry<String, String> ent : headers.entrySet()){
				LOG.debug("Header=" + ent.getKey() + ": " + ent.getValue());
				request.append(ent.getKey());
				request.append(": ");
				request.append(ent.getValue());
				request.append("\r\n");
			}
			request.append("\r\n");
			LOG.debug(request.toString());
			
			final OutputStream os = sock.getOutputStream();
			os.write(request.toString().getBytes());
			
			if(!"".equals(body)){
				LOG.debug(body);
				os.write(body.getBytes());
			}
			
			
			final InputStream is = sock.getInputStream();
			final BufferedReader read = new BufferedReader(new InputStreamReader(is));
			
			final String response = read.readLine();
			final String[] respParts = response.split(" ", 3);
			final int code = Integer.valueOf(respParts[1]);
			
			final HashMap<String, String> responseFields = new HashMap<>();
			String line = "";
			while((line = read.readLine()) != null){
				if("".equals(line)){
					break;
				}
				final String[] parts = line.split(": ");
				responseFields.put(parts[0], parts[1]);
			}
			LOG.debug(responseFields);
			
			int readLen = -1;
			if(responseFields.containsKey("Content-Length")){
				readLen = Integer.valueOf(responseFields.get("Content-Length"));
			}
			final StringBuffer outBuf = new StringBuffer();
			int chr = 0;
			while(readLen-- > 0 && (chr = read.read()) != -1){
				outBuf.append((char)chr);
			}
			
			sock.close();
			LOG.debug(outBuf.toString());
			LOG.debug("Response code: " + code);
			return new ByteArrayInputStream(outBuf.toString().getBytes());
		} catch (IOException e) {
			LOG.error(e);
		} catch (HTTPException e) {
			LOG.error(e);
		}
		return null;
	}
	
	/**
	 * Generates a application/x-www-form-urlencoded string for the
	 * body of the request.
	 * @return
	 */
	public String generateBody(){
		final StringBuffer buf = new StringBuffer();
		
		final int len = formElems.size();
		int pos = 0;
		
		for(Map.Entry<String, String> ent : formElems.entrySet()){
			buf.append(ent.getKey());
			buf.append("=");
			buf.append(ent.getValue());
			if(++pos != len){
				buf.append("&");
			}
		}
		
		return buf.toString();
	}
}
