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

import org.exoplatform.portal.tree.list.ListTree;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * The context of a node.
 */
public final class NodeContext<N> extends ListTree<NodeContext<N>>
{

   /** . */
   final TreeContext<N> tree;

   /** . */
   final N node;

   /** The internal id, either the persistent id or a sequence id. */
   String id;

   /** node data representing persistent state. */
   NodeData data;

   /** The new state if any. */
   NodeState state;

   /** . */
   private boolean hidden;

   /** . */
   private int hiddenCount;

   /** . */
   String name;

   /** . */
   private boolean expanded;

   NodeContext(NodeModel<N> model, NodeData data)
   {
      this.id = data.id;
      this.name = data.getName();
      this.tree = new TreeContext<N>(model, this);
      this.node = tree.model.create(this);
      this.data = data;
      this.state = null;
      this.hidden = false;
      this.hiddenCount = 0;
      this.expanded = false;
   }

   private NodeContext(TreeContext<N> tree, NodeData data)
   {
      this.id = data.id;
      this.name = data.getName();
      this.tree = tree;
      this.node = tree.model.create(this);
      this.data = data;
      this.state = null;
      this.hidden = false;
      this.hiddenCount = 0;
      this.expanded = false;
   }

   private NodeContext(TreeContext<N> tree, String name, NodeState state)
   {
      this.id = Integer.toString(tree.sequence++);
      this.name = name;
      this.tree = tree;
      this.node = tree.model.create(this);
      this.data = null;
      this.state = state;
      this.hidden = false;
      this.hiddenCount = 0;
      this.expanded = false;
   }

   NodeData toData()
   {
      String parentId = data.parentId;
      String id = data.id;
      String name = getName();
      NodeState state = this.state != null ? this.state : data.state;
      String[] children;
      if (expanded)
      {
         children = new String[getSize()];
         int index = 0;
         for (NodeContext<N> current = getFirst();current != null;current = current.getNext())
         {
            children[index++] = current.data.id;
         }
      }
      else
      {
         children = data.children;
      }
      return new NodeData(parentId, id, name, state, children);
   }

   /**
    * Returns true if the tree containing this node has pending transient changes.
    *
    * @return true if there are uncommited changes
    */
   public boolean hasChanges()
   {
      return tree.hasChanges();
   }

   /**
    * Returns the relative depth of this node with respect to the ancestor argument.
    *
    * @param ancestor the ancestor
    * @return the depth
    * @throws IllegalArgumentException if the ancestor argument is not an ancestor
    * @throws NullPointerException if the ancestor argument is null
    */
   public int getDepth(NodeContext<N> ancestor) throws IllegalArgumentException, NullPointerException
   {
      if (ancestor == null)
      {
         throw new NullPointerException();
      }
      int depth = 0;
      for (NodeContext<N> current = this;current != null;current = current.getParent())
      {
         if (current == ancestor)
         {
            return depth;
         }
         else
         {
            depth++;
         }
      }
      throw new IllegalArgumentException("Context " + ancestor + " is not an ancestor of " + this);
   }

   public NodeContext<N> getDescendant(String id) throws NullPointerException
   {
      if (id == null)
      {
         throw new NullPointerException();
      }

      //
      NodeContext<N> found = null;
      if (this.id.equals(id))
      {
         found = this;
      }
      else
      {
         if (expanded)
         {
            for (NodeContext<N> current = getFirst();current != null;current = current.getNext())
            {
               found = current.getDescendant(id);
               if (found != null)
               {
                  break;
               }
            }
         }
      }
      return found;
   }

   /**
    * Returns true if the context is currently hidden.
    *
    * @return the hidden value
    */
   public boolean isHidden()
   {
      return hidden;
   }

   /**
    * Updates the hiddent value.
    *
    * @param hidden the hidden value
    */
   public void setHidden(boolean hidden)
   {
      if (this.hidden != hidden)
      {
         NodeContext<N> parent = getParent();
         if (parent != null)
         {
            if (hidden)
            {
               parent.hiddenCount++;
            }
            else
            {
               parent.hiddenCount--;
            }
         }
         this.hidden = hidden;
      }
   }

   /**
    * Applies a filter recursively, the filter will update the hiddent status of the
    * fragment.
    *
    * @param filter the filter to apply
    */
   public void filter(NodeFilter filter)
   {
      doFilter(0, filter);
   }

   private void doFilter(int depth, NodeFilter filter)
   {
      boolean accept = filter.accept(depth, getId(), name, getState());
      setHidden(!accept);
      if (expanded)
      {
         for (NodeContext<N> node = getFirst();node != null;node = node.getNext())
         {
            node.doFilter(depth + 1, filter);
         }
      }
   }

   /**
    * Returns the associated node with this context
    *
    * @return the node
    */
   public N getNode()
   {
      return node;
   }

   /**
    * Reutrns the context id or null if the context is not associated with a persistent navigation node.
    *
    * @return the id
    */
   public String getId()
   {
      return data != null ? data.getId() : null;
   }

   /**
    * Returns the context index among its parent.
    *
    * @return the index value
    */
   public int getIndex()
   {
      int count = 0;
      for (NodeContext<N> node = getPrevious();node != null;node = node.getPrevious())
      {
         count++;
      }
      return count;
   }

   public String getName()
   {
      return name;
   }

   /**
    * Rename this context.
    *
    * @param name the new name
    * @throws NullPointerException if the name is null
    * @throws IllegalStateException if the parent is null
    * @throws IllegalArgumentException if the parent already have a child with the specified name
    */
   public void setName(String name) throws NullPointerException, IllegalStateException, IllegalArgumentException
   {
      NodeContext<N> parent = getParent();
      if (parent == null)
      {
         throw new IllegalStateException("Cannot rename a node when its parent is not visible");
      }
      else
      {
         NodeContext<N> blah = parent.get(name);
         if (blah != null)
         {
            if (blah == this)
            {
               // We do nothing
            }
            else
            {
               throw new IllegalArgumentException("the node " + name + " already exist");
            }
         }
         else
         {
            this.name = name;
            tree.addChange(new NodeChange.Renamed<N>(this, name));
         }
      }
   }

   public NodeContext<N> get(String name) throws NullPointerException, IllegalStateException
   {
      if (name == null)
      {
         throw new NullPointerException();
      }
      if (!expanded)
      {
         throw new IllegalStateException("No children relationship");
      }

      //
      for (NodeContext<N> node = getFirst();node != null;node = node.getNext())
      {
         if (node.name.equals(name))
         {
            return node;
         }
      }

      //
      return null;
   }

   /**
    * Returns the total number of nodes.
    *
    * @return the total number of nodes
    */
   public int getNodeSize()
   {
      if (expanded)
      {
         return getSize();
      }
      else
      {
         return data.children.length;
      }
   }

   /**
    * Returns the node count defined by:
    * <ul>
    *    <li>when the node has a children relationship, the number of non hidden nodes</li>
    *    <li>when the node has not a children relationship, the total number of nodes</li>
    * </ul>
    *
    * @return the node count
    */
   public int getNodeCount()
   {
      if (expanded)
      {
         return getSize() - hiddenCount;
      }
      else
      {
         return data.children.length;
      }
   }

   public NodeState getState()
   {
      if (state != null)
      {
         return state;
      }
      else if (data != null)
      {
         return data.getState();
      }
      else
      {
         return null;
      }
   }

   /**
    * Update the context state
    *
    * @param state the new state
    * @throws NullPointerException if the state is null
    */
   public void setState(NodeState state) throws NullPointerException
   {
      if (state == null)
      {
         throw new NullPointerException("No null state accepted");
      }

      //
      tree.addChange(new NodeChange.Updated<N>(this, state));

      //
      this.state = state;
   }

   public N getParentNode()
   {
      NodeContext<N> parent = getParent();
      return parent != null ? parent.node : null;
   }

   public N getNode(String name) throws NullPointerException
   {
      NodeContext<N> child = get(name);
      return child != null && !child.hidden ? child.node: null;
   }

   public N getNode(int index)
   {
      if (index < 0)
      {
         throw new IndexOutOfBoundsException("Index " + index + " cannot be negative");
      }
      if (!expanded)
      {
         throw new IllegalStateException("No children relationship");
      }
      NodeContext<N> context = getFirst();
      while (context != null && (context.hidden || index-- > 0))
      {
         context = context.getNext();
      }
      if (context == null)
      {
         throw new IndexOutOfBoundsException("Index " + index + " is out of bounds");
      }
      else
      {
         return context.node;
      }
   }

   public final Iterator<N> iterator()
   {
      return new Iterator<N>()
      {
         NodeContext<N> next = getFirst();
         {
            while (next != null && next.isHidden())
            {
               next = next.getNext();
            }
         }
         public boolean hasNext()
         {
            return next != null;
         }
         public N next()
         {
            if (next != null)
            {
               NodeContext<N> tmp = next;
               do
               {
                  next = next.getNext();
               }
               while (next != null && next.isHidden());
               return tmp.getNode();
            }
            else
            {
               throw new NoSuchElementException();
            }
         }
         public void remove()
         {
            throw new UnsupportedOperationException();
         }
      };
   }

   /** . */
   private Collection<N> nodes;

   public Collection<N> getNodes()
   {
      if (expanded)
      {
         if (nodes == null)
         {
            nodes = new AbstractCollection<N>()
            {
               public Iterator<N> iterator()
               {
                  return NodeContext.this.iterator();
               }
               public int size()
               {
                  return getNodeCount();
               }
            };
         }
         return nodes;
      }
      else
      {
         return null;
      }
   }

   /**
    * Add a child node at the specified index with the specified name. If the index argument
    * is null then the node is added at the last position among the children otherwise
    * the node is added at the specified index.
    *
    * @param index the index
    * @param name the node name
    * @return the created node
    * @throws NullPointerException if the model or the name is null
    * @throws IndexOutOfBoundsException if the index is negative or greater than the children size
    * @throws IllegalStateException if the children relationship does not exist
    */
   public NodeContext<N> add(Integer index, String name) throws NullPointerException, IndexOutOfBoundsException, IllegalStateException
   {
      if (name == null)
      {
         throw new NullPointerException("No null name accepted");
      }

      //
      NodeContext<N> nodeContext = new NodeContext<N>(tree, name, new NodeState.Builder().capture());
      nodeContext.expand();
      _add(index, nodeContext);
      return nodeContext;
   }

   /**
    * Move a context as a child context of this context at the specified index. If the index argument
    * is null then the context is added at the last position among the children otherwise
    * the context is added at the specified index.
    *
    * @param index the index
    * @param context the context to move
    * @throws NullPointerException if the model or the context is null
    * @throws IndexOutOfBoundsException if the index is negative or greater than the children size
    * @throws IllegalStateException if the children relationship does not exist
    */
   public void add(Integer index, NodeContext<N> context) throws NullPointerException, IndexOutOfBoundsException, IllegalStateException
   {
      if (context == null)
      {
         throw new NullPointerException("No null context argument accepted");
      }

      //
      _add(index, context);
   }

   public NodeContext<N> insertLast(NodeData data)
   {
      if (data == null)
      {
         throw new NullPointerException("No null data argument accepted");
      }

      //
      NodeContext<N> context = new NodeContext<N>(tree, data);
      insertLast(context);
      return context;
   }

   public NodeContext<N> insertAt(Integer index, NodeData data)
   {
      if (data == null)
      {
         throw new NullPointerException("No null data argument accepted");
      }

      //
      NodeContext<N> context = new NodeContext<N>(tree, data);
      insertAt(index, context);
      return context;
   }

   public NodeContext<N> insertAfter(NodeData data)
   {
      if (data == null)
      {
         throw new NullPointerException("No null data argument accepted");
      }

      //
      NodeContext<N> context = new NodeContext<N>(tree, data);
      insertAfter(context);
      return context;
   }

   private void _add(final Integer index, NodeContext<N> child)
   {
      NodeContext<N> previousParent = child.getParent();

      //
      if (index == null)
      {
         NodeContext<N> before = getLast();
         while (before != null && before.isHidden())
         {
            before = before.getPrevious();
         }
         if (before == null)
         {
            insertAt(0, child);
         }
         else
         {
            before.insertAfter(child);
         }
      }
      else if (index < 0)
      {
         throw new IndexOutOfBoundsException("No negative index accepted");
      }
      else if (index == 0)
      {
         insertAt(0, child);
      }
      else
      {
         NodeContext<N> before = getFirst();
         if (before == null)
         {
            throw new IndexOutOfBoundsException("Index " + index + " is greater than 0");
         }
         for (int count = index;count > 1;count -= before.isHidden() ? 0 : 1)
         {
            before = before.getNext();
            if (before == null)
            {
               throw new IndexOutOfBoundsException("Index " + index + " is greater than the number of children " + (index - count));
            }
         }
         before.insertAfter(child);
      }

      //
      if (previousParent != null)
      {
         tree.addChange(new NodeChange.Moved<N>(previousParent, this, child.getPrevious(), child));
      }
      else
      {
         tree.addChange(new NodeChange.Created<N>(this, child.getPrevious(), child, child.name));
      }
   }

   /**
    * Remove a specified context.
    *
    * @param name the name of the context to remove
    * @return true if the context was removed
    * @throws NullPointerException if the name argument is null
    * @throws IllegalArgumentException if the named context does not exist
    * @throws IllegalStateException if the children relationship does not exist
    */
   public boolean removeNode(String name) throws NullPointerException, IllegalArgumentException, IllegalStateException
   {
      NodeContext<N> node = get(name);
      if (node == null)
      {
         throw new IllegalArgumentException("Cannot remove non existent " + name + " child");
      }

      //
      if (node.hidden)
      {
         return false;
      }
      else
      {
         node.remove();

         //
         tree.addChange(new NodeChange.Destroyed<N>(this, node));

         //
         return true;
      }
   }

   public boolean isExpanded()
   {
      return expanded;
   }

   void expand()
   {
      if (!expanded)
      {
         this.expanded = true;
      }
      else
      {
         throw new IllegalStateException("Context is already expanded");
      }
   }

   Iterable<NodeContext<N>> getContexts()
   {
      if (expanded)
      {
         return new Iterable<NodeContext<N>>()
         {
            public Iterator<NodeContext<N>> iterator()
            {
               return listIterator();
            }
         };
      }
      else
      {
         return null;
      }
   }

   protected void beforeRemove(NodeContext<N> context)
   {
      if (!expanded)
      {
         throw new IllegalStateException();
      }
   }

   protected void beforeInsert(NodeContext<N> context)
   {
      if (!expanded)
      {
         throw new IllegalStateException("No children relationship");
      }

      //
      if (!tree.editMode)
      {
         NodeContext<N> existing = get(context.name);
         if (existing != null && existing != context)
         {
            throw new IllegalArgumentException("Tree " + context.name + " already in the map");
         }
      }
   }

   protected void afterInsert(NodeContext<N> context)
   {
      super.afterInsert(context);

      //
      if (context.hidden)
      {
         hiddenCount++;
      }
   }

   protected void afterRemove(NodeContext<N> context)
   {
      if (context.hidden)
      {
         hiddenCount--;
      }

      //
      super.afterRemove(context);
   }
}
