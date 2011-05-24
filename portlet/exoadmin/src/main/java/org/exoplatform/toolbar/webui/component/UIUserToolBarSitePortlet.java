/**
 * Copyright (C) 2009 eXo Platform SAS.
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

package org.exoplatform.toolbar.webui.component;

import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PageNode;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.webui.navigation.PageNavigationUtils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : Pham Thanh Tung
 *          thanhtungty@gmail.com
 * May 26, 2009  
 */
@ComponentConfig(lifecycle = UIApplicationLifecycle.class, template = "app:/groovy/admintoolbar/webui/component/UIUserToolBarSitePortlet.gtmpl"

)
public class UIUserToolBarSitePortlet extends UIPortletApplication
{

   public UIUserToolBarSitePortlet() throws Exception
   {
   }

   public List<String> getAllPortalNames() throws Exception
   {
      UserPortalConfigService dataStorage = getApplicationComponent(UserPortalConfigService.class);
      return dataStorage.getAllPortalNames();
   }

   public String getCurrentPortal()
   {
      return Util.getPortalRequestContext().getPortalOwner();
   }

   public String getPortalURI(String portalName)
   {
      String currentPortalURI = Util.getPortalRequestContext().getPortalURI();
      return currentPortalURI.substring(0, currentPortalURI.lastIndexOf(getCurrentPortal())) + portalName + "/";
   }

   public Collection<UserNode> getCurrentPortalNavigation() throws Exception
   {
      UIPortalApplication uiApp = Util.getUIPortalApplication();
      UserPortal userPortal = uiApp.getUserPortalConfig().getUserPortal();
      UserNavigation nav = userPortal.getNavigation(SiteKey.portal(getCurrentPortal()));
      if (nav != null)
      {
         UserNode rootNodes =  userPortal.getNode(nav, Scope.NAVIGATION);
         if (rootNodes != null)
         {
            PageNavigationUtils.filter(rootNodes, Util.getPortalRequestContext().getRemoteUser());
            return rootNodes.getChildren();
         }
      }
      return Collections.emptyList();
   }

   public PageNode getSelectedPageNode() throws Exception
   {
      return Util.getUIPortal().getSelectedNode();
   }
}