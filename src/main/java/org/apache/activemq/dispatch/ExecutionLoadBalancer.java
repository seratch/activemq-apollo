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
package org.apache.activemq.dispatch;

import org.apache.activemq.dispatch.IDispatcher.DispatchContext;

public interface ExecutionLoadBalancer {

    /**
     * A Load Balanced Dispatch context can be moved between different
     * dispatchers.
     */
    public interface LoadBalancedDispatchContext extends DispatchContext {
        /**
         * A dispatcher must call this when it starts dispatch for this context
         */
        public void startingDispatch();

        /**
         * A dispatcher must call this when it has finished dispatching a
         * context
         */
        public void finishedDispatch();

        /**
		 * 
		 */
        public void processForeignUpdates();
    }

    public interface PoolableDispatchContext extends DispatchContext {

        public void setLoadBalancedDispatchContext(LoadBalancedDispatchContext context);

        /**
         * Indicates that another thread has made an update to the dispatch
         * context.
         * 
         */
        public void onForeignThreadUpdate();

        public PoolableDispatcher getDispatcher();
    }

    public interface PoolableDispatcher extends IDispatcher {

        /**
         * Indicates that another thread has made an update to the dispatch
         * context.
         * 
         */
        public PoolableDispatchContext createPoolablDispatchContext(Dispatchable dispatchable, String name);
    }

    /**
     * This wraps the dispatch context into one that is load balanced by the
     * LoadBalancer
     * 
     * @param context
     *            The context to wrap.
     * @return
     */
    public LoadBalancedDispatchContext createLoadBalancedDispatchContext(PoolableDispatchContext context);

    /**
     * Adds a Dispatcher to the list of dispatchers managed by the load balancer
     * 
     * @param dispatcher
     */
    public void addDispatcher(PoolableDispatcher dispatcher);

    /**
     * A Dispatcher must call this from it's dispatcher thread to indicate that
     * is has started it's dispatch has started.
     */
    public void onDispatcherStarted(PoolableDispatcher dispatcher);

    /**
     * A Dispatcher must call this from it's dispatcher thread when exiting it's
     * dispatch loop
     */
    public void onDispatcherStopped(PoolableDispatcher dispatcher);
}
