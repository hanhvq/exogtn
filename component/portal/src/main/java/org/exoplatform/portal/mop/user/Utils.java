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

package org.exoplatform.portal.mop.user;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
class Utils
{

   static String[] parsePath(String path)
   {
      // Where we start
      final int start = 0 < path.length() && path.charAt(0) == '/' ? 1 : 0;

      //
      if (start == path.length())
      {
         return null;
      }

      // Where we end
      int end = path.length();
      while (end > start && path.charAt(end - 1) == '/')
      {
         end--;
      }


      // Count the number of slash
      int count = 0;
      int i = start;
      while (true)
      {
         int pos = path.indexOf('/', i);
         if (pos == -1)
         {
            pos = end;
         }
         if (pos == end)
         {
            if (pos > i)
            {
               count++;
            }
            break;
         }
         else
         {
            count++;
            i = pos + 1;
         }
      }

      //
      String[] names = new String[count];
      i = start;
      int index = 0;
      while (true)
      {
         int pos = path.indexOf('/', i);
         if (pos == -1)
         {
            pos = end;
         }
         if (pos == end)
         {
            if (pos > i)
            {
               names[index] = path.substring(i, end);
            }
            break;
         }
         else
         {
            if (i < pos)
            {
               names[index++] = path.substring(i, pos);
            }
            else
            {
               names[index++] = "";
            }
            i = pos + 1;
         }
      }

      //
      return names;
   }

}
