/*
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
 */
package com.captiveimagination.jgn.server;

import java.io.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.message.player.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class ServerPlayerMessageListener implements MessageListener {
	private NetworkingServer server;
	
	public ServerPlayerMessageListener(NetworkingServer server) {
		this.server = server;
	}
	
	public void messageReceived(Message message) {
	}
	
	public void messageReceived(PlayerMessage message) {
        JGNPlayer player = server.getPlayer(message.getPlayerId());
        
        if (player != null) {
            // Check to see if the player has a port assigned for this message server
            if ((((Message)message).getMessageServer() instanceof TCPMessageServer) && (player.getTCPPort() == -1)) {
                player.setTCPPort(((Message)message).getRemotePort());
            } else if ((((Message)message).getMessageServer() instanceof UDPMessageServer) && (player.getUDPPort() == -1)) {
                player.setUDPPort(((Message)message).getRemotePort());
            }
        	
            // Update the player to say that it has been heard from
            player.heardFrom();
        }
	}
	
	public void messageReceived(PlayerJoinRequestMessage message) {
        try {
            server.joinRequest(message);
        } catch(IOException exc) {
            // TODO create exception handling for server player message listener
            exc.printStackTrace();
        }
	}
	
	public void messageReceived(PlayerDisconnectMessage message) {
		server.disconnectRequest(message);
	}

	public int getListenerMode() {
		return MessageListener.ALL;
	}
}
