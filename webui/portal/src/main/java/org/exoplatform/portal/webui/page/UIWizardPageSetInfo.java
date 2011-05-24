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

package org.exoplatform.portal.webui.page;

import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.webui.navigation.UIPageNodeSelector;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIWizard;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormDateTimeInput;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.DateTimeValidator;
import org.exoplatform.webui.form.validator.IdentifierValidator;
import org.exoplatform.webui.form.validator.MandatoryValidator;
import org.exoplatform.webui.form.validator.StringLengthValidator;

import java.util.Calendar;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Thi Hoa
 *          hoa.nguyen@exoplatform.com
 * Oct 31, 2006  
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "system:/groovy/portal/webui/page/UIWizardPageSetInfo.gtmpl", events = {
   @EventConfig(listeners = UIWizardPageSetInfo.ChangeNodeActionListener.class, phase = Phase.DECODE),
   @EventConfig(listeners = UIWizardPageSetInfo.SwitchVisibleActionListener.class, phase = Phase.DECODE),
   @EventConfig(listeners = UIWizardPageSetInfo.SwitchPublicationDateActionListener.class, phase = Phase.DECODE)})
public class UIWizardPageSetInfo extends UIForm
{

   final public static String PAGE_NAME = "pageName";

   final public static String PAGE_DISPLAY_NAME = "pageDisplayName";

   final public static String VISIBLE = "visible";

   final public static String SHOW_PUBLICATION_DATE = "showPublicationDate";

   final public static String START_PUBLICATION_DATE = "startPublicationDate";

   final public static String END_PUBLICATION_DATE = "endPublicationDate";

   private boolean isEditMode = false;

   private boolean firstTime = true;

   public UIWizardPageSetInfo() throws Exception
   {
      UIFormCheckBoxInput<Boolean> uiDateInputCheck =
         new UIFormCheckBoxInput<Boolean>(SHOW_PUBLICATION_DATE, null, false);
      UIFormCheckBoxInput<Boolean> uiVisibleCheck = new UIFormCheckBoxInput<Boolean>(VISIBLE, null, false);
      uiDateInputCheck.setOnChange("SwitchPublicationDate");
      uiVisibleCheck.setOnChange("SwitchVisible");

      addChild(UIPageNodeSelector.class, null, null);
      addUIFormInput(new UIFormStringInput(PAGE_NAME, "name", null).addValidator(MandatoryValidator.class)
         .addValidator(StringLengthValidator.class, 3, 30).addValidator(IdentifierValidator.class));
      addUIFormInput(new UIFormStringInput(PAGE_DISPLAY_NAME, "label", null).setMaxLength(255).addValidator(
         StringLengthValidator.class, 3, 120));
      addUIFormInput(uiVisibleCheck.setChecked(true));
      addUIFormInput(uiDateInputCheck);
      addUIFormInput(new UIFormDateTimeInput(START_PUBLICATION_DATE, null, null).addValidator(DateTimeValidator.class));
      addUIFormInput(new UIFormDateTimeInput(END_PUBLICATION_DATE, null, null).addValidator(DateTimeValidator.class));
   }

   //TODO: it looks like this method is not used
   public void setEditMode() throws Exception
   {
      isEditMode = true;
      UIFormStringInput uiNameInput = getChildById(PAGE_NAME);
      uiNameInput.setEditable(false);
   }

   public boolean isEditMode()
   {
      return isEditMode;
   }

   public void invokeSetBindingBean(Object bean) throws Exception
   {
      super.invokeSetBindingBean(bean);
      UserNode node = (UserNode)bean;
      
      Visibility visibility;
      if (getUIFormCheckBoxInput(VISIBLE).isChecked())
      {
         UIFormCheckBoxInput showPubDate = getUIFormCheckBoxInput(SHOW_PUBLICATION_DATE);
         visibility = showPubDate.isChecked() ?  Visibility.TEMPORAL : Visibility.DISPLAYED;
      }
      else
      {
         visibility = Visibility.HIDDEN;
      }
      node.setVisibility(visibility);

      Calendar cal = getUIFormDateTimeInput(START_PUBLICATION_DATE).getCalendar();
      long time = (cal != null) ? cal.getTimeInMillis() : -1;
      node.setStartPublicationTime(time);
      cal = getUIFormDateTimeInput(END_PUBLICATION_DATE).getCalendar();
      time = (cal != null) ? cal.getTimeInMillis() : -1;
      node.setEndPublicationTime(time);
   }

   public UserNode createUserNode(UserNode parent) throws Exception
   {
      UserNode child = parent.addChild("temp");
      invokeSetBindingBean(child);
      if (child.getLabel() == null || child.getLabel().trim().length() == 0)
      {
         child.setLabel(child.getName());
      }
      return child;
   }

   public void setShowCheckPublicationDate(boolean show)
   {
      getUIFormCheckBoxInput(VISIBLE).setChecked(show);
      UIFormCheckBoxInput<Boolean> uiForm = getUIFormCheckBoxInput(SHOW_PUBLICATION_DATE);
      uiForm.setRendered(show);
      setShowPublicationDate(show && uiForm.isChecked());
   }

   public void setShowPublicationDate(boolean show)
   {
      getUIFormDateTimeInput(START_PUBLICATION_DATE).setRendered(show);
      getUIFormDateTimeInput(END_PUBLICATION_DATE).setRendered(show);
   }

   public UserNode getSelectedPageNode()
   {
      UIPageNodeSelector uiPageNodeSelector = getChild(UIPageNodeSelector.class);
      return uiPageNodeSelector.getSelectedNode();
   }

   public void processRender(WebuiRequestContext context) throws Exception
   {
      if (isEditMode && getChild(UIPageNodeSelector.class).getSelectedNode() == null)
         reset();
      super.processRender(context);
   }

   public boolean isFirstTime()
   {
      return firstTime;
   }

   public void setFirstTime(boolean firstTime)
   {
      this.firstTime = firstTime;
   }

   static public class ChangeNodeActionListener extends EventListener<UIWizardPageSetInfo>
   {
      public void execute(Event<UIWizardPageSetInfo> event) throws Exception
      {
         String uri = event.getRequestContext().getRequestParameter(OBJECTID);

         UIPageNodeSelector uiPageNodeSelector = event.getSource().getChild(UIPageNodeSelector.class);
         uiPageNodeSelector.setSelectedURI(uri);

         UIPortalApplication uiPortalApp = uiPageNodeSelector.getAncestorOfType(UIPortalApplication.class);
         UIWizard uiWizard = uiPortalApp.findFirstComponentOfType(UIWizard.class);
         event.getRequestContext().addUIComponentToUpdateByAjax(uiWizard);
      }
   }

   static public class SwitchPublicationDateActionListener extends EventListener<UIWizardPageSetInfo>
   {
      public void execute(Event<UIWizardPageSetInfo> event) throws Exception
      {
         UIWizardPageSetInfo uiForm = event.getSource();
         boolean isCheck = uiForm.getUIFormCheckBoxInput(SHOW_PUBLICATION_DATE).isChecked();
         uiForm.getUIFormDateTimeInput(START_PUBLICATION_DATE).setRendered(isCheck);
         uiForm.getUIFormDateTimeInput(END_PUBLICATION_DATE).setRendered(isCheck);
         UIWizard uiWizard = uiForm.getAncestorOfType(UIWizard.class);
         event.getRequestContext().addUIComponentToUpdateByAjax(uiWizard);
      }

   }

   static public class SwitchVisibleActionListener extends EventListener<UIWizardPageSetInfo>
   {
      @Override
      public void execute(Event<UIWizardPageSetInfo> event) throws Exception
      {
         UIWizardPageSetInfo uiForm = event.getSource();
         boolean isCheck = uiForm.getUIFormCheckBoxInput(VISIBLE).isChecked();
         uiForm.setShowCheckPublicationDate(isCheck);
         event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
      }
   }

}
