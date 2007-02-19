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
 * Created: Jan 3, 2007
 */

package com.captiveimagination.jgn.convert;

import java.nio.ByteBuffer;

import com.captiveimagination.jgn.MessageClient;

/**
 * @author Alfons Seul
 * @author Nathan Sweet <misc@n4te.com>
 */
public class EnumConverter extends Converter {
	public void writeObjectData (MessageClient client, Object object, ByteBuffer buffer) throws ConversionException {
		buffer.putInt(((Enum)object).ordinal());
	}

	public Object readObjectData (ByteBuffer buffer, Class c) throws ConversionException {
		Object[] enumConstants = c.getEnumConstants();
		if (enumConstants == null) throw new ConversionException("Class is not an enum: " + c.getName());
		int ordinal = buffer.getInt();
		if (ordinal < 0 || ordinal > enumConstants.length - 1)
			throw new ConversionException("Invalid ordinal for enum \"" + c.getName() + "\": " + ordinal);
		return enumConstants[ordinal];
	}
}
