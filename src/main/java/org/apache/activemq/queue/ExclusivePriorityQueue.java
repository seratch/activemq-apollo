/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.queue;

import org.apache.activemq.dispatch.PriorityLinkedList;
import org.apache.activemq.flow.Flow;
import org.apache.activemq.flow.FlowController;
import org.apache.activemq.flow.ISourceController;
import org.apache.activemq.flow.PriorityFlowController;
import org.apache.kahadb.util.LinkedNode;

/**
 */
public class ExclusivePriorityQueue<E> extends AbstractFlowQueue<E> implements IFlowQueue<E> {

    private final PriorityLinkedList<PriorityNode> queue;
    private Mapper<Integer, E> priorityMapper;

    private class PriorityNode extends LinkedNode<PriorityNode> {
        E elem;
        int prio;
    }

    private final PriorityFlowController<E> controller;

    /**
     * Creates a flow queue that can handle multiple flows.
     * 
     * @param priority
     * @param flow
     *            The {@link Flow}
     * @param capacity
     * @param resume
     * @param controller
     *            The FlowController if this queue is flow controlled:
     */
    public ExclusivePriorityQueue(int priority, Flow flow, String name, int capacity, int resume) {
        super(name);
        this.queue = new PriorityLinkedList<PriorityNode>(10);
        this.controller = new PriorityFlowController<E>(priority, getFlowControllableHook(), flow, this, capacity, resume);

    }

    public boolean offer(E elem, ISourceController<E> source) {
        return controller.offer(elem, source);
    }

    /**
     * Performs a limited add to the queue.
     */
    public final void add(E elem, ISourceController<E> source) {
        controller.add(elem, source);
    }

    /**
     * Called when the controller accepts a message for this queue.
     */
    public synchronized void flowElemAccepted(ISourceController<E> controller, E elem) {
        PriorityNode node = new PriorityNode();
        node.elem = elem;
        node.prio = priorityMapper.map(elem);

        queue.add(node, node.prio);
        notifyReady();
    }

    public FlowController<E> getFlowController(Flow flow) {
        // TODO:
        return null;
    }

    public boolean isDispatchReady() {
        return !queue.isEmpty();
    }

    public boolean pollingDispatch() {
        PriorityNode node = null;
        synchronized (this) {
            node = queue.poll();
            // FIXME the release should really be done after dispatch.
            // doing it here saves us from having to resynchronize
            // after dispatch, but release limiter space too soon.
            if (autoRelease && node != null) {
                controller.elementDispatched(node.elem);
            }
        }

        if (node != null) {
            drain.drain(node.elem, controller);
            return true;
        } else {
            return false;
        }
    }

    public Mapper<Integer, E> getPriorityMapper() {
        return priorityMapper;
    }

    public void setPriorityMapper(Mapper<Integer, E> priorityMapper) {
        this.priorityMapper = priorityMapper;
        controller.setPriorityMapper(priorityMapper);
    }

    @Override
    public String toString() {
        return getResourceName();
    }
}
