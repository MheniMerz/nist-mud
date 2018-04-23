/*
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

package gov.nist.antd.flowmon.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Instruction;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;

/**
 * Flow commit wrapper.
 */
public class FlowCommitWrapper {
	private static final Logger LOG = LoggerFactory.getLogger(FlowCommitWrapper.class);

	private DataBroker dataBrokerService;

	class MyFutureCallback implements FutureCallback<Void> {
		public boolean success;
		public Semaphore semaphore = new Semaphore(0);
		private FlowBuilder flow;

		public MyFutureCallback(FlowBuilder flow) {
			this.flow = flow;
		}

		@Override
		public void onSuccess(Void aVoid) {
			LOG.info("Write of flow on device succeeded. flow = " + flow.getFlowName());
			semaphore.release();
			success = true;

		}

		@Override
		public void onFailure(Throwable throwable) {
			LOG.error("Write of flow on device failed. flow = " + flow.getFlowName(), throwable);
			semaphore.release();
			success = false;
		}

		public Semaphore getSemaphore() {
			return this.semaphore;
		}
	};

	public FlowCommitWrapper(DataBroker dataBrokerService) {
		this.dataBrokerService = dataBrokerService;
	}

	public CheckedFuture<Void, TransactionCommitFailedException> writeFlowToConfig(InstanceIdentifier<Flow> flowPath,
			Flow flowBody) {
		ReadWriteTransaction addFlowTransaction = dataBrokerService.newReadWriteTransaction();
		// mdsalApiManager.addFlowToTx(dpnId, flowBody, addFlowTransaction);
		addFlowTransaction.merge(LogicalDatastoreType.CONFIGURATION, flowPath, flowBody, true);
		return addFlowTransaction.submit();
	}

	public boolean deleteFlow(FlowKey flowKey, short tableId, InstanceIdentifier<FlowCapableNode> flowNodeIdent) {
		ReadWriteTransaction modification = dataBrokerService.newReadWriteTransaction();
		LOG.info("deleteFlow : " + flowKey.getId().getValue());
		final InstanceIdentifier<Flow> path = flowNodeIdent.child(Table.class, new TableKey(tableId)).child(Flow.class,
				flowKey);
		modification.delete(LogicalDatastoreType.CONFIGURATION, path);
		CheckedFuture<Void, TransactionCommitFailedException> commitFuture = modification.submit();
		try {
			commitFuture.checkedGet();
		} catch (TransactionCommitFailedException e) {
			return false;
		}
		return true;
	}

	public boolean flowExists(String flowIdPrefix, short tableId, InstanceIdentifier<FlowCapableNode> flowNodeIdent) {
		InstanceIdentifier<Table> tableInstanceId = flowNodeIdent.child(Table.class, new TableKey(tableId));
		CheckedFuture<Optional<Table>, ReadFailedException> commitFuture = dataBrokerService.newReadOnlyTransaction()
				.read(LogicalDatastoreType.CONFIGURATION, tableInstanceId);
		try {
			Set<Table> tableSet = commitFuture.get().asSet();
			for (Table table : tableSet) {
				List<Flow> flows = table.getFlow();
				for (Flow flow : flows) {
					if (flow.getId().getValue().startsWith(flowIdPrefix)) {
						return true;
					}
				}
			}
			return false;
		} catch (InterruptedException e) {
			LOG.error("Error reading flows ", e);
			return false;
		} catch (ExecutionException e) {
			LOG.error("Error reading flows ", e);
			return false;
		}
	}

	public List<FlowKey> readFlows(InstanceIdentifier<FlowCapableNode> flowNodeIdent, short tableId, String uriPrefix,
			MacAddress sourceMacAddress) {

		List<FlowKey> retval = new ArrayList<FlowKey>();

		InstanceIdentifier<Table> tableInstanceId = flowNodeIdent.child(Table.class, new TableKey(tableId));
		CheckedFuture<Optional<Table>, ReadFailedException> commitFuture = dataBrokerService.newReadOnlyTransaction()
				.read(LogicalDatastoreType.CONFIGURATION, tableInstanceId);
		try {
			Set<Table> tableSet = commitFuture.get().asSet();
			for (Table table : tableSet) {
				List<Flow> flows = table.getFlow();
				for (Flow flow : flows) {
					String flowId = flow.getId().getValue();
					if (sourceMacAddress != null) {
						MacAddress flowSourceMacAddress = null;

						if (flow.getMatch() != null && flow.getMatch().getEthernetMatch() != null
								&& flow.getMatch().getEthernetMatch().getEthernetSource() != null) {
							flowSourceMacAddress = flow.getMatch().getEthernetMatch().getEthernetSource().getAddress();
						}

						if (flowSourceMacAddress != null && flowId.startsWith(uriPrefix)
								&& flowSourceMacAddress.equals(sourceMacAddress)) {
							retval.add(flow.getKey());
						}
					} else {
						if (flowId.startsWith(uriPrefix)) {
							retval.add(flow.getKey());
						}
					}
				}
			}
			return retval;
		} catch (InterruptedException e) {
			LOG.error("Error reading flows ", e);
			return null;
		} catch (ExecutionException e) {
			LOG.error("Error reading flows ", e);
			return null;
		}
	}

	public synchronized void writeFlow(FlowBuilder flow, InstanceIdentifier<FlowCapableNode> flowNodeIdent) {
		writeFlow(flow.build(), flowNodeIdent);
	}

	public synchronized void writeFlow(Flow flow, InstanceIdentifier<FlowCapableNode> flowNodeIdent) {
		ReadWriteTransaction modification = dataBrokerService.newReadWriteTransaction();
		LOG.info("writeFlow : " + flowNodeIdent + " Flow : " + flow.getFlowName() + " tableId " + flow.getTableId()
		+ " flowId " + flow.getId().getValue());

		LOG.info(flow.toString());
		for (Instruction instruction : flow.getInstructions().getInstruction()) {
			LOG.info("writeFlow: Instruction =  " + instruction.getInstruction().toString());
		}

		final InstanceIdentifier<Flow> path1 = flowNodeIdent.child(Table.class, new TableKey(flow.getTableId()))
				.child(Flow.class, flow.getKey());

		modification.merge(LogicalDatastoreType.CONFIGURATION, path1, flow, true);
		modification.submit();
	}

	/**
	 * Delete the flows corresponding to a MUD uri and sourceMacAddress.
	 *
	 * @param flowCapableNode
	 *            -- the node from which to delete the flows.
	 * @param uri
	 *            -- the mud URI
	 * @param sourceMacAddress
	 *            -- the device source mac address.
	 */

	public synchronized void deleteFlows(InstanceIdentifier<FlowCapableNode> flowCapableNode, String uri, short table,
			MacAddress sourceMacAddress) {

		LOG.info("deleteFlows " + uri);
		List<FlowKey> flowKeys = readFlows(flowCapableNode, table, uri, sourceMacAddress);
		for (FlowKey flowKey : flowKeys) {
			deleteFlow(flowKey, table, flowCapableNode);
		}
	}

}
