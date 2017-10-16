/*
 * Copyright 2016-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.distributed;

import com.facebook.buck.distributed.thrift.CoordinatorService;
import com.facebook.buck.distributed.thrift.FinishedBuildingRequest;
import com.facebook.buck.distributed.thrift.FinishedBuildingResponse;
import com.facebook.buck.distributed.thrift.GetTargetsToBuildRequest;
import com.facebook.buck.distributed.thrift.GetTargetsToBuildResponse;
import com.facebook.buck.distributed.thrift.StampedeId;
import com.facebook.buck.log.Logger;
import com.facebook.buck.slb.ThriftException;
import com.google.common.base.Preconditions;
import java.io.Closeable;
import javax.annotation.Nullable;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;

public class ThriftCoordinatorClient implements Closeable {
  private static final Logger LOG = Logger.get(ThriftCoordinatorClient.class);

  private final String remoteHost;
  private final int remotePort;
  private final StampedeId stampedeId;

  @Nullable private TFramedTransport transport;
  @Nullable private CoordinatorService.Client client;

  public ThriftCoordinatorClient(String remoteHost, int remotePort, StampedeId stampedeId) {
    this.remoteHost = Preconditions.checkNotNull(remoteHost);
    this.remotePort = remotePort;
    this.stampedeId = stampedeId;
  }

  /** Starts the thrift client. */
  public ThriftCoordinatorClient start() throws ThriftException {
    transport = new TFramedTransport(new TSocket(remoteHost, remotePort));

    try {
      transport.open();
    } catch (TTransportException e) {
      throw new ThriftException(e);
    }

    TProtocol protocol = new TBinaryProtocol(transport);
    client = new CoordinatorService.Client(protocol);
    return this;
  }

  public ThriftCoordinatorClient stop() {
    Preconditions.checkNotNull(transport, "The client has already been stopped.");
    transport.close();
    transport = null;
    client = null;
    return this;
  }

  /** Gets the next set of targets to build for a given Minion. */
  public GetTargetsToBuildResponse getTargetsToBuild(String minionId) throws ThriftException {
    LOG.debug(String.format("Minion [%s] is requesting targets to build.", minionId));
    Preconditions.checkNotNull(client, "Client was not started.");
    GetTargetsToBuildRequest request =
        new GetTargetsToBuildRequest().setMinionId(minionId).setStampedeId(stampedeId);
    try {
      GetTargetsToBuildResponse response = client.getTargetsToBuild(request);
      return response;
    } catch (TException e) {
      throw new ThriftException(e);
    }
  }

  public FinishedBuildingResponse finishedBuilding(String minionId, int minionExitCode)
      throws ThriftException {
    LOG.debug(String.format("Minion [%s] is reporting that it finished building.", minionId));
    Preconditions.checkNotNull(client, "Client was not started.");
    FinishedBuildingRequest request =
        new FinishedBuildingRequest()
            .setStampedeId(stampedeId)
            .setMinionId(minionId)
            .setBuildExitCode(minionExitCode);
    try {
      FinishedBuildingResponse response = client.finishedBuilding(request);
      return response;
    } catch (TException e) {
      throw new ThriftException(e);
    }
  }

  @Override
  public void close() throws ThriftException {
    if (client != null) {
      stop();
    }
  }
}
