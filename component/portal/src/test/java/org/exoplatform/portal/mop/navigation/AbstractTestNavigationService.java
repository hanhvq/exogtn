/*
 * Copyright (C) 2011 eXo Platform SAS.
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

package org.exoplatform.portal.mop.navigation;

import junit.framework.AssertionFailedError;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.AbstractPortalTest;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.pom.config.POMSessionManager;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public abstract class AbstractTestNavigationService extends AbstractPortalTest
{

   /** . */
   protected POMSessionManager mgr;

   /** . */
   protected NavigationServiceImpl service;

   /** . */
   protected DataStorage dataStorage;

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      //
      PortalContainer container = PortalContainer.getInstance();
      mgr = (POMSessionManager)container.getComponentInstanceOfType(POMSessionManager.class);
      service = new NavigationServiceImpl(mgr);
      dataStorage = (DataStorage)container.getComponentInstanceOfType(DataStorage.class);
      //
      begin();
   }

   protected void sync()
   {
      end();
      begin();
   }

   protected void sync(boolean save)
   {
      end(save);
      begin();
   }

   @Override
   protected void end(boolean save)
   {
      if (save)
      {
         try
         {
            startService();
            super.end(save);
         }
         finally
         {
            stopService();
         }
      }
      else
      {
         super.end(save);
      }
   }

   @Override
   protected void tearDown() throws Exception
   {
      end();
      super.tearDown();
   }

   private void startService()
   {
      try
      {
         begin();
         service.clearCache();
         end();
      }
      catch (Exception e)
      {
         AssertionFailedError afe = new AssertionFailedError();
         afe.initCause(e);
         throw afe;
      }
   }

   private void stopService()
   {
      begin();
      service.clearCache();
      end();
   }
}
