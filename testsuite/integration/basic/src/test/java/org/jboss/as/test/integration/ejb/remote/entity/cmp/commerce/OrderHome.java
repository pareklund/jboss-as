/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ejb.remote.entity.cmp.commerce;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Set;
import javax.ejb.CreateException;
import javax.ejb.EJBHome;
import javax.ejb.FinderException;

public interface OrderHome extends EJBHome {
    Order create() throws CreateException, RemoteException;

    Order create(Long id) throws CreateException, RemoteException;

    Order findByPrimaryKey(Long ordernumber) throws FinderException, RemoteException;

    Collection findByStatus(String status) throws FinderException, RemoteException;

    Collection findAll() throws FinderException, RemoteException;

    Collection findDoubleJoin(int a, int b) throws FinderException, RemoteException;

    Collection findWithLimitOffset(int offset, int limit) throws FinderException, RemoteException;

    Set getStuff(String jbossQl, Object[] arguments)
            throws FinderException, RemoteException;

    Set selectLazy(String jbossQl, Object[] arguments)
            throws FinderException, RemoteException;

    void resetId() throws RemoteException;
}
