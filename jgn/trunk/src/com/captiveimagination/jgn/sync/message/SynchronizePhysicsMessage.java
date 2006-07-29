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
 * Created: Jul 27, 2006
 */
package com.captiveimagination.jgn.sync.message;

/**
 * An extension of Synchronize3DMessage that adds physics information in addition
 * to the position and rotational synchronization information.
 * 
 * @author Matthew D. Hicks
 */
public class SynchronizePhysicsMessage extends Synchronize3DMessage {
	private float forceX;
	private float forceY;
	private float forceZ;
	private float torqueX;
	private float torqueY;
	private float torqueZ;
	private float linearVelocityX;
	private float linearVelocityY;
	private float linearVelocityZ;
	private float angularVelocityX;
	private float angularVelocityY;
	private float angularVelocityZ;

	public float getAngularVelocityX() {
		return angularVelocityX;
	}

	public void setAngularVelocityX(float angularVelocityX) {
		this.angularVelocityX = angularVelocityX;
	}

	public float getAngularVelocityY() {
		return angularVelocityY;
	}

	public void setAngularVelocityY(float angularVelocityY) {
		this.angularVelocityY = angularVelocityY;
	}

	public float getAngularVelocityZ() {
		return angularVelocityZ;
	}

	public void setAngularVelocityZ(float angularVelocityZ) {
		this.angularVelocityZ = angularVelocityZ;
	}

	public float getForceX() {
		return forceX;
	}

	public void setForceX(float forceX) {
		this.forceX = forceX;
	}

	public float getForceY() {
		return forceY;
	}

	public void setForceY(float forceY) {
		this.forceY = forceY;
	}

	public float getForceZ() {
		return forceZ;
	}

	public void setForceZ(float forceZ) {
		this.forceZ = forceZ;
	}

	public float getLinearVelocityX() {
		return linearVelocityX;
	}

	public void setLinearVelocityX(float linearVelocityX) {
		this.linearVelocityX = linearVelocityX;
	}

	public float getLinearVelocityY() {
		return linearVelocityY;
	}

	public void setLinearVelocityY(float linearVelocityY) {
		this.linearVelocityY = linearVelocityY;
	}

	public float getLinearVelocityZ() {
		return linearVelocityZ;
	}

	public void setLinearVelocityZ(float linearVelocityZ) {
		this.linearVelocityZ = linearVelocityZ;
	}

	public float getTorqueX() {
		return torqueX;
	}

	public void setTorqueX(float torqueX) {
		this.torqueX = torqueX;
	}

	public float getTorqueY() {
		return torqueY;
	}

	public void setTorqueY(float torqueY) {
		this.torqueY = torqueY;
	}

	public float getTorqueZ() {
		return torqueZ;
	}

	public void setTorqueZ(float torqueZ) {
		this.torqueZ = torqueZ;
	}
}