/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gov.nist.antd.sdnmud.impl.dhcp;

import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.Inet4Address;
import java.nio.ByteBuffer;

/**
 * This class implements the DHCP-REQUEST packet.
 */
public class DhcpRequestPacket extends DhcpPacket {

    /*
     * The MUD URL.
     */
    private String mudUrl;

    /**
     * Generates a REQUEST packet with the specified parameters.
     */
    DhcpRequestPacket(int transId, InetAddress clientIp, byte[] clientMac,
            boolean broadcast) {
        super(transId, clientIp, INET4_ADDRESS_ANY, INET4_ADDRESS_ANY,
                INET4_ADDRESS_ANY, clientMac, broadcast);
    }

    @Override
    public String toString() {
        String s = super.toString();
        return s + " REQUEST, desired IP " + mRequestedIp + " from host '"
                + mHostName + "', param list length "
                + (mRequestedParams == null ? 0 : mRequestedParams.length);
    }

    /**
     * Fills in a packet with the requested REQUEST attributes.
     */
    @Override
    public ByteBuffer buildPacket(int encap, short destUdp, short srcUdp) {
        ByteBuffer result = ByteBuffer.allocate(MAX_LENGTH);

        fillInPacket(encap, INET4_ADDRESS_ALL, INET4_ADDRESS_ANY, destUdp,
                srcUdp, result, DHCP_BOOTREQUEST, mBroadcast);
        result.flip();
        return result;
    }

    /**
     * Adds the optional parameters to the client-generated REQUEST packet.
     */
    @Override
    void finishPacket(ByteBuffer buffer) {
        byte[] clientId = new byte[7];

        // assemble client identifier
        clientId[0] = CLIENT_ID_ETHER;
        System.arraycopy(mClientMac, 0, clientId, 1, 6);

        addTlv(buffer, DHCP_MESSAGE_TYPE, DHCP_MESSAGE_TYPE_REQUEST);
        addTlv(buffer, DHCP_PARAMETER_LIST, mRequestedParams);
        addTlv(buffer, DHCP_REQUESTED_IP, mRequestedIp);
        addTlv(buffer, DHCP_SERVER_IDENTIFIER, mServerIdentifier);
        addTlv(buffer, DHCP_CLIENT_IDENTIFIER, clientId);
        addTlvEnd(buffer);
    }

    /**
     * Notifies the specified state machine of the REQUEST packet parameters.
     */
    public void doNextOp(DhcpStateMachine machine) {
        InetAddress clientRequest = mRequestedIp == null
                ? mClientIp
                : mRequestedIp;
        LOG.debug(TAG, "requested IP is " + mRequestedIp + " and client IP is "
                + mClientIp);
        machine.onRequestReceived(mBroadcast, mTransId, mClientMac,
                clientRequest, mRequestedParams, mHostName);
    }

    /**
     * @param mudUrl
     */
    public void setMudUrl(String mudUrl) {
        this.mudUrl = mudUrl;

    }

    public String getMudUrl() {
        return this.mudUrl;
    }
}
