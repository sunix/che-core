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
package org.eclipse.che.commons.json;

@SuppressWarnings("serial")
public class JsonParseException extends Exception {
    public JsonParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonParseException(String message) {
        super(message);
    }

    public JsonParseException(Throwable cause) {
        super(cause);
    }
}