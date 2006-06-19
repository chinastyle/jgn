/**
 * Copyright (c) 2005-2006 JavaGameNetworking
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'JavaGameNetworking' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Created: Jun 3, 2006
 */
package com.captiveimagination.jgn.convert;

import java.io.*;
import java.lang.reflect.*;
import java.nio.*;
import java.util.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.message.*;

/**
 * ConversionHandlers exist to process incoming
 * and outgoing Messages
 * 
 * @author Matthew D. Hicks
 */
public class ConversionHandler {
	private static final MethodComparator methodComparator = new MethodComparator();
	public static final HashSet<String> ignore = new HashSet<String>();
	static {
		ignore.add("getId");
	}
	
	private Converter[] converters;
	private Method[] getters;
	private Method[] setters;
	private Class messageClass;
	
	private ConversionHandler(Converter[] converters, Method[] getters, Method[] setters, Class messageClass) {
		this.converters = converters;
		this.getters = getters;
		this.setters = setters;
		this.messageClass = messageClass;
	}
	
	public Message receiveMessage(ByteBuffer buffer) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, SecurityException, NoSuchMethodException {
		Message message = (Message)messageClass.newInstance();
		for (int i = 0; i < converters.length; i++) {
			converters[i].set(message, setters[i], buffer);
		}
		return message;
	}
	
	public void sendMessage(Message message, ByteBuffer buffer) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, IOException {
		// Write the message type
		buffer.putShort(message.getMessageClient().getMessageTypeId(message.getClass()));
		for (int i = 0; i < converters.length; i++) {
			converters[i].get(message, getters[i], buffer);
		}
	}
	
	public static final synchronized ConversionHandler getConversionHandler(Class<? extends Message> messageClass) {
		initConverters();
		
		// TODO if the message implements UniqueMessage then we need to associate the getId first
		
		// Introspect Class
		ArrayList<Converter> converters = new ArrayList<Converter>();
		ArrayList<Method> getters = new ArrayList<Method>();
		ArrayList<Method> setters = new ArrayList<Method>();
		Method[] ms = messageClass.getMethods();
		ArrayList<Method> methods = new ArrayList<Method>();
		Collections.addAll(methods, ms);
		Collections.sort(methods, methodComparator);
		for (Method getter : methods) {
			if (!getter.getName().startsWith("get")) continue;	// Make sure it's a getter
			if (ignore.contains(getter.getName())) continue;	// Methods to be ignored
			
			String name = getter.getName().substring(3);
			Method setter = null;
			for (Method m : methods) {
				if (m.getName().equals("set" + name)) {
					if ((m.getParameterTypes().length == 1) && (m.getParameterTypes()[0] == getter.getReturnType())) {
						setter = m;
						break;
					}
				}
			}
			
			if (setter == null) continue;
			
			Converter converter = Converter.CONVERTERS.get(getter.getReturnType());
			if (converter != null) {
				converters.add(converter);
				getter.setAccessible(true);
				setter.setAccessible(true);
				getters.add(getter);
				setters.add(setter);
			}
		}
		
		return new ConversionHandler(converters.toArray(new Converter[converters.size()]), getters.toArray(new Method[getters.size()]), setters.toArray(new Method[setters.size()]), messageClass);
	}
	
	public static final void initConverters() {
		if (Converter.CONVERTERS.size() == 0) {
			Converter.CONVERTERS.put(boolean.class, new BooleanConverter());
			Converter.CONVERTERS.put(byte.class, new ByteConverter());
			Converter.CONVERTERS.put(char.class, new CharacterConverter());
			Converter.CONVERTERS.put(short.class, new ShortConverter());
			Converter.CONVERTERS.put(int.class, new IntegerConverter());
			Converter.CONVERTERS.put(long.class, new LongConverter());
			Converter.CONVERTERS.put(float.class, new FloatConverter());
			Converter.CONVERTERS.put(double.class, new DoubleConverter());
			
			Converter.CONVERTERS.put(String.class, new StringConverter());
			Converter.CONVERTERS.put(String[].class, new StringArrayConverter());
			
			Converter.CONVERTERS.put(boolean[].class, new BooleanArrayConverter());
			Converter.CONVERTERS.put(byte[].class, new ByteArrayConverter());
			Converter.CONVERTERS.put(char[].class, new CharacterArrayConverter());
			Converter.CONVERTERS.put(short[].class, new ShortArrayConverter());
			Converter.CONVERTERS.put(int[].class, new IntegerArrayConverter());
			Converter.CONVERTERS.put(long[].class, new LongArrayConverter());
			Converter.CONVERTERS.put(float[].class, new FloatArrayConverter());
			Converter.CONVERTERS.put(double[].class, new DoubleArrayConverter());
		}
	}
}
