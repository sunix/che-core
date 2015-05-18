/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.machine.server;

import com.google.common.base.Objects;

import org.eclipse.che.api.machine.server.spi.ExposedPort;

/**
 * @author Alexander Garagatyi
 */
public class ExposedPortImpl implements ExposedPort {
    private int    externalPort;
    private int    internalPort;
    private String protocol;
    private String ip;

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public ExposedPortImpl withHost(String host) {
        this.ip = host;
        return this;
    }

    public String getProtocol() {
        return this.protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public ExposedPortImpl withProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public int getInternalPort() {
        return this.internalPort;
    }

    public void setInternalPort(int internalPort) {
        this.internalPort = internalPort;
    }

    public ExposedPortImpl withInternalPort(int internalPort) {
        this.internalPort = internalPort;
        return this;
    }

    public int getExternalPort() {
        return this.externalPort;
    }

    public void setExternalPort(int externalPort) {
        this.externalPort = externalPort;
    }

    public ExposedPortImpl withExternalPort(int externalPort) {
        this.externalPort = externalPort;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExposedPortImpl)) return false;
        ExposedPortImpl that = (ExposedPortImpl)o;
        return Objects.equal(externalPort, that.externalPort) &&
               Objects.equal(internalPort, that.internalPort) &&
               Objects.equal(protocol, that.protocol) &&
               Objects.equal(ip, that.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(externalPort, internalPort, protocol, ip);
    }
}
