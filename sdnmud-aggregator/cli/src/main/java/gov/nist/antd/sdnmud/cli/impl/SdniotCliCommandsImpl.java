/*
 * Copyright © 2017 MIST and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package gov.nist.antd.sdnmud.cli.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nist.antd.sdnmud.cli.api.SdniotCliCommands;

public class SdniotCliCommandsImpl implements SdniotCliCommands {

    private static final Logger LOG = LoggerFactory.getLogger(SdniotCliCommandsImpl.class);
    private final DataBroker dataBroker;

    public SdniotCliCommandsImpl(final DataBroker db) {
        this.dataBroker = db;
        LOG.info("SdniotCliCommandImpl initialized");
    }

    @Override
    public Object testCommand(Object testArgument) {

        return "This is a test implementation of test-command";

    }
}