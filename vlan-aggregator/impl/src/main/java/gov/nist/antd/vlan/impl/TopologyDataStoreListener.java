/* Copyright (c) None.
 * This file includes code developed by employees of the National Institute of
 * Standards and Technology (NIST)
 *
 * This software was developed by employees of the National Institute of
 * Standards and Technology (NIST), and others. This software has been
 * contributed to the public domain. Pursuant to title 15 Untied States
 * Code Section 105, works of NIST employees are not subject to copyright
 * protection in the United States and are considered to be in the public
 * domain. As a result, a formal license is not needed to use this software.
 *
 * This software is provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
 * NON-INFRINGEMENT AND DATA ACCURACY. NIST does not warrant or make any
 * representations regarding the use of the software or the results thereof,
 * including but not limited to the correctness, accuracy, reliability or
 * usefulness of this software.
 */

package gov.nist.antd.vlan.impl;

import java.util.Collection;

import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.nist.params.xml.ns.yang.nist.network.topology.rev170915.Links;
import org.opendaylight.yang.gen.v1.urn.nist.params.xml.ns.yang.nist.network.topology.rev170915.Topology;

class TopologyDataStoreListener implements DataTreeChangeListener<Topology> {

    private VlanProvider vlanProvider;

    TopologyDataStoreListener(VlanProvider vlanProvider) {
        this.vlanProvider = vlanProvider;
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Topology>> changes) {
        for (DataTreeModification<Topology> change : changes) {
            Links topology = change.getRootNode().getDataAfter();
            this.vlanProvider.setLinks(topology);
        }
        if (vlanProvider.getWakeupListener() != null && this.vlanProvider.isConfigured() ) {
            this.vlanProvider.getWakeupListener().installInitialFlows();
        }
        
        this.vlanProvider.getOpenstackStacksConfigDataStoreListener().setupOpenstackStacks();
    }

}
