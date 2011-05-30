/*
 * Copyright (C) 2010 eXo Platform SAS.
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

package org.exoplatform.portal.mop.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.mop.navigation.NavigationServiceException;
import org.exoplatform.portal.mop.navigation.NodeChangeListener;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeContextChangeAdapter;
import org.exoplatform.portal.mop.navigation.NodeFilter;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.navigation.VisitMode;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class UserPortalImpl implements UserPortal
{

   /**
    * A context that always return null.
    */
   private static final UserPortalContext NULL_CONTEXT = new UserPortalContext()
   {
      public ResourceBundle getBundle(UserNavigation navigation)
      {
         return null;
      }
   };

   /** . */
   final UserPortalConfigService service;

   /** . */
   final NavigationService navigationService;
   
   /** . */
   private final OrganizationService organizationService;

   /** . */
   final UserACL acl;
   
   /** . */
   private final PortalConfig portal;

   /** . */
   final UserPortalContext context;

   /** . */
   final String userName;

   /** . */
   private List<UserNavigation> navigations;

   /** . */
   private final String portalName;

   public UserPortalImpl(
      UserPortalConfigService service,
      NavigationService navigationService,
      OrganizationService organizationService,
      UserACL acl,
      String portalName,
      PortalConfig portal,
      String userName,
      UserPortalContext context)
   {
      // So we don't care about testing nullity
      if (context == null)
      {
         context = NULL_CONTEXT;
      }

      //
      this.service = service;
      this.navigationService = navigationService;
      this.organizationService = organizationService;
      this.acl = acl;
      this.portalName = portalName;
      this.portal = portal;
      this.userName = userName;
      this.context = context;
      this.navigations = null;
   }
   /**
    * Returns an immutable sorted list of the valid navigations related to the user.
    *
    * @return the navigations
    * @throws UserPortalException any user portal exception
    */
   public List<UserNavigation> getNavigations() throws UserPortalException, NavigationServiceException
   {
      if (navigations == null)
      {
         List<UserNavigation> navigations = new ArrayList<UserNavigation>(userName == null ? 1 : 10);
         NavigationContext portalNav = navigationService.loadNavigation(new SiteKey(SiteType.PORTAL, portalName));
         navigations.add(new UserNavigation(
            this,
            portalNav,
            acl.hasEditPermissionOnNavigation(portalNav.getKey())));

         //
         if (userName != null)
         {
            // Add user nav if any
            NavigationContext userNavigation = navigationService.loadNavigation(SiteKey.user(userName));
            if (userNavigation != null && userNavigation.getState() != null)
            {
               navigations.add(new UserNavigation(this, userNavigation, true));
            }

            //
            Collection<?> groups;
            try
            {
               if (acl.getSuperUser().equals(userName))
               {
                  groups = organizationService.getGroupHandler().getAllGroups();
               }
               else
               {
                  groups = organizationService.getGroupHandler().findGroupsOfUser(userName);
               }
            }
            catch (Exception e)
            {
               throw new UserPortalException("Could not retrieve groups", e);
            }

            //
            for (Object group : groups)
            {
               Group m = (Group)group;
               String groupId = m.getId().trim();
               if (!groupId.equals(acl.getGuestsGroup()))
               {
                  NavigationContext groupNavigation = navigationService.loadNavigation(SiteKey.group(groupId));
                  if (groupNavigation != null && groupNavigation.getState() != null)
                  {
                     navigations.add(new UserNavigation(
                        this,
                        groupNavigation,
                        acl.hasEditPermissionOnNavigation(groupNavigation.getKey())));
                  }
               }
            }

            // Sort the list finally
            Collections.sort(navigations, new Comparator<UserNavigation>()
            {
               public int compare(UserNavigation nav1, UserNavigation nav2)
               {
                  return nav1.getPriority() - nav2.getPriority();
               }
            });
         }

         //
         this.navigations = Collections.unmodifiableList(navigations);
      }
      return navigations;
   }

   public UserNavigation getNavigation(SiteKey key) throws NullPointerException, UserPortalException, NavigationServiceException
   {
      if (key == null)
      {
         throw new NullPointerException("No null key accepted");
      }
      for (UserNavigation navigation : getNavigations())
      {
         if (navigation.getKey().equals(key))
         {
            return navigation;
         }
      }

      //
      return null;
   }

   public UserNode getNode(
      UserNavigation userNavigation,
      Scope scope,
      UserNodeFilterConfig filterConfig,
      NodeChangeListener<UserNode> listener) throws NullPointerException, UserPortalException, NavigationServiceException
   {
      UserNodeContext context = new UserNodeContext(userNavigation, filterConfig);
      NodeContext<UserNode> nodeContext = navigationService.loadNode(context, userNavigation.navigation, scope, NodeContextChangeAdapter.safeWrap(listener));
      if (nodeContext != null)
      {
         return nodeContext.getNode().filter();
      }
      else
      {
         return null;
      }
   }

   public void updateNode(UserNode node, Scope scope, NodeChangeListener<UserNode> listener) 
      throws NullPointerException, IllegalArgumentException, UserPortalException, NavigationServiceException
   {
      if (node == null)
      {
         throw new NullPointerException("No null node accepted");
      }
      navigationService.updateNode(node.context, scope, NodeContextChangeAdapter.safeWrap(listener));
      node.filter();
   }
   
   public void rebaseNode(UserNode node, Scope scope, NodeChangeListener<UserNode> listener)
      throws NullPointerException, IllegalArgumentException, UserPortalException, NavigationServiceException
   {
      if (node == null)
      {
         throw new NullPointerException("No null node accepted");
      }
      navigationService.rebaseNode(node.context, scope, NodeContextChangeAdapter.safeWrap(listener));
      node.filter();
   }

   public void saveNode(UserNode node, NodeChangeListener<UserNode> listener) throws NullPointerException, UserPortalException, NavigationServiceException
   {
      if (node == null)
      {
         throw new NullPointerException("No null node accepted");
      }
      navigationService.saveNode(node.context, NodeContextChangeAdapter.safeWrap(listener));
      node.filter();
   }

   private class MatchingScope implements Scope
   {
      final UserNavigation userNavigation;
      final UserNodeFilterConfig filterConfig;
      final String[] match;
      int score;
      String id;
      UserNode userNode;

      MatchingScope(UserNavigation userNavigation, UserNodeFilterConfig filterConfig, String[] match)
      {
         this.userNavigation = userNavigation;
         this.filterConfig = filterConfig;
         this.match = match;
      }

      void resolve() throws NavigationServiceException
      {
         UserNodeContext context = new UserNodeContext(userNavigation, filterConfig);
         NodeContext<UserNode> nodeContext = navigationService.loadNode(context, userNavigation.navigation, this, null);
         if (context != null)
         {
            if (score > 0)
            {
               userNode = nodeContext.getNode().filter().find(id);
            }
         }
      }

      public Visitor get()
      {
         return new Visitor()
         {
            public VisitMode enter(int depth, String id, String name, NodeState state)
            {
               if (depth == 0 && "default".equals(name))
               {
                  score = 0;
                  MatchingScope.this.id = null;
                  return VisitMode.ALL_CHILDREN;
               }
               else if (depth <= match.length && name.equals(match[depth - 1]))
               {
                  score++;
                  MatchingScope.this.id = id;
                  return VisitMode.ALL_CHILDREN;
               }
               else
               {
                  return VisitMode.NO_CHILDREN;
               }
            }

            public void leave(int depth, String id, String name, NodeState state)
            {
            }
         };
      }
   }

   public UserNode getDefaultPath(UserNodeFilterConfig filterConfig) throws UserPortalException, NavigationServiceException
   {
      for (UserNavigation userNavigation : getNavigations())
      {
         NavigationContext navigation = userNavigation.navigation;
         if (navigation.getState() != null)
         {
            UserNodeContext context = new UserNodeContext(userNavigation, filterConfig);
            NodeContext<UserNode> nodeContext = navigationService.loadNode(context, navigation, Scope.CHILDREN, null);
            if (nodeContext != null)
            {
               UserNode root = nodeContext.getNode().filter();
               for (UserNode node : root.getChildren())
               {
                  return node;
               }
            }
         }
      }

      //
      return null;
   }

   public UserNode resolvePath(UserNodeFilterConfig filterConfig, String path)
      throws NullPointerException, UserPortalException, NavigationServiceException
   {
      if (path == null)
      {
         throw new NullPointerException("No null path accepted");
      }

      //  Parse path
      String[] segments = Utils.parsePath(path);

      // Find the first navigation available or return null
      if (segments == null)
      {
         return getDefaultPath(null);
      }

      // Get navigations
      List<UserNavigation> navigations = getNavigations();

      //
      MatchingScope best = null;
      for (UserNavigation navigation : navigations)
      {
         MatchingScope scope = new MatchingScope(navigation, filterConfig, segments);
         scope.resolve();
         if (scope.score == segments.length)
         {
            best = scope;
            break;
         }
         else
         {
            if (best == null)
            {
               best = scope;
            }
            else
            {
               if (scope.score > best.score)
               {
                  best = scope;
               }
            }
         }
      }

      //
      if (best != null && best.score > 0)
      {
         return best.userNode;
      }
      else
      {
         return getDefaultPath(null);
      }
   }

   public UserNode resolvePath(UserNavigation navigation, UserNodeFilterConfig filterConfig, String path)
      throws NullPointerException, UserPortalException, NavigationServiceException
   {
      if (navigation == null)
      {
         throw new NullPointerException("No null navigation accepted");
      }
      if (path == null)
      {
         throw new NullPointerException("No null path accepted");
      }

      //
      String[] segments = Utils.parsePath(path);

      //
      if (segments == null)
      {
         return null;
      }

      //
      MatchingScope scope = new MatchingScope(navigation, filterConfig, segments);
      scope.resolve();

      //
      if (scope.score > 0)
      {
         return scope.userNode;
      }
      else
      {
         return null;
      }
   }

   public NodeFilter createFilter(UserNodeFilterConfig predicate)
   {
      return new UserNodeFilter(this, predicate);
   }
}
