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

import org.exoplatform.commons.utils.ExpressionUtil;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.navigation.NavigationServiceException;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeFilter;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.gatein.common.text.EntityEncoder;

import java.util.Collection;
import java.util.Collections;

/**
 * A navigation node as seen by a user.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class UserNode
{

   /** . */
   final UserNavigation navigation;

   /** . */
   final NodeContext<UserNode> context;

   /** . */
   private String resolvedLabel;

   /** . */
   private String encodedResolvedLabel;

   /** . */
   private String uri;

   UserNode(UserNavigation navigation, NodeContext<UserNode> context)
   {
      this.navigation = navigation;
      this.context = context;
      this.resolvedLabel = null;
      this.encodedResolvedLabel = null;
      this.uri = null;
   }

   public String getId()
   {
      return context.getId();
   }

   public UserNode filter(NodeFilter filter)
   {
      context.filter(filter);
      return this;
   }

   public String getName()
   {
      return context.getName();
   }

   public void setName(String name)
   {
      context.setName(name);
   }

   public String getURI()
   {
      if (uri == null)
      {
         uri = buildURI().toString();
      }
      return uri;
   }

   private StringBuilder buildURI()
   {
      UserNode parent = context.getParentNode();
      if (parent != null)
      {
         StringBuilder builder = parent.buildURI();
         if (builder.length() > 0)
         {
            builder.append('/');
         }
         return builder.append(context.getName());
      }
      else
      {
         return new StringBuilder();
      }
   }

   public String getLabel()
   {
      return context.getState().getLabel();
   }

   public void setLabel(String label)
   {
      this.resolvedLabel = null;
      this.encodedResolvedLabel = null;

      //
      context.setState(new NodeState.Builder(context.getState()).setLabel(label).capture());
   }

   public String getIcon()
   {
      return context.getState().getIcon();
   }

   public void setIcon(String icon)
   {
      context.setState(new NodeState.Builder(context.getState()).setIcon(icon).capture());
   }

   public long getStartPublicationTime()
   {
      return context.getState().getStartPublicationTime();
   }

   public void setStartPublicationTime(long startPublicationTime)
   {
      context.setState(new NodeState.Builder(context.getState()).setStartPublicationTime(startPublicationTime).capture());
   }

   public long getEndPublicationTime()
   {
      return context.getState().getEndPublicationTime();
   }

   public void setEndPublicationTime(long endPublicationTime)
   {
      context.setState(new NodeState.Builder(context.getState()).setEndPublicationTime(endPublicationTime).capture());
   }

   public Visibility getVisibility()
   {
      return context.getState().getVisibility();
   }

   public void setVisibility(Visibility visibility)
   {
      context.setState(new NodeState.Builder(context.getState()).setVisibility(visibility).capture());
   }

   public String getPageRef()
   {
      return context.getState().getPageRef();
   }

   public void setPageRef(String pageRef)
   {
      context.setState(new NodeState.Builder(context.getState()).setPageRef(pageRef).capture());
   }

   public String getResolvedLabel()
   {
      if (resolvedLabel == null)
      {
         String resolvedLabel;
         if (navigation.bundle != null && context.getState().getLabel() != null)
         {
            resolvedLabel = ExpressionUtil.getExpressionValue(navigation.bundle, context.getState().getLabel());
         }
         else
         {
            resolvedLabel = null;
         }

         //
         if (resolvedLabel == null)
         {
            resolvedLabel = getName();
         }

         //
         this.resolvedLabel = resolvedLabel;
      }
      return resolvedLabel;
   }

   public String getEncodedResolvedLabel()
   {
      if (encodedResolvedLabel == null)
      {
         encodedResolvedLabel = EntityEncoder.FULL.encode(getResolvedLabel());
      }
      return encodedResolvedLabel;
   }

   public UserNode getParent()
   {
      return context.getParentNode();
   }

   /**
    * Returns true if the children relationship determined.
    *
    * @return ture if node has children
    */
   public boolean hasChildrenRelationship()
   {
      return context.isExpanded();
   }

   /**
    * Returns the number of children.
    *
    * @return the number of children
    */
   public int getChildrenCount()
   {
      return context.getNodeCount();
   }

   public int getChildrenSize()
   {
      return context.getNodeSize();
   }

   public Collection<UserNode> getChildren()
   {
      return context.isExpanded() ? context.getNodes() : Collections.<UserNode>emptyList();
   }

   /**
    * Returns a child by its name or null if the child does not exist or the children relationship has not been loaded.
    *
    * @param childName the child name
    * @return the corresponding user node
    * @throws NullPointerException if the child name is null
    */
   public UserNode getChild(String childName) throws NullPointerException
   {
      if (context.isExpanded())
      {
         return context.getNode(childName);
      }
      else
      {
         return null;
      }
   }

   /**
    * Returns a child by its index or null if the children relationship has not been loaded.
    *
    * @param childIndex the child index
    * @return the corresponding user node
    * @throws IndexOutOfBoundsException if the children relationship is loaded and the index is outside of its bounds
    */
   public UserNode getChild(int childIndex) throws IndexOutOfBoundsException
   {
      if (context.isExpanded())
      {
         return context.getNode(childIndex);
      }
      else
      {
         return null;
      }
   }

   public void addChild(UserNode child)
   {
      context.add(null, child.context);
   }

   public void addChild(int index, UserNode child)
   {
      context.add(index, child.context);
   }

   public UserNode addChild(String childName)
   {
      return context.add(null, childName).getNode();
   }

   public boolean removeChild(String childName)
   {
      return context.removeNode(childName);
   }

   public void save() throws NavigationServiceException
   {
      navigation.portal.navigationService.saveNode(context);
   }

   // Keep this internal for now
   UserNode find(String nodeId)
   {
      NodeContext<UserNode> found = context.getDescendant(nodeId);
      return found != null ? found.getNode() : null;
   }
}
