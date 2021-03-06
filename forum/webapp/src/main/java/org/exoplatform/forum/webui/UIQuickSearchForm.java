/***************************************************************************
 * Copyright (C) 2003-2008 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 ***************************************************************************/
package org.exoplatform.forum.webui;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.forum.ForumUtils;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.service.ForumSearchResult;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.UserProfile;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormStringInput;

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/templates/forum/webui/UIQuickSearchForm.gtmpl",
    events = {
      @EventConfig(listeners = UIQuickSearchForm.SearchActionListener.class)
    }
)
public class UIQuickSearchForm extends BaseForumForm {
  final static private String FIELD_SEARCHVALUE  = "inputValue";


  public UIQuickSearchForm() throws Exception {
    UIFormStringInput form = new UIFormStringInput(FIELD_SEARCHVALUE, FIELD_SEARCHVALUE, null);
    addChild(form);
  }
  
  protected void initPlaceholder() throws Exception {
    getUIStringInput(FIELD_SEARCHVALUE).setHTMLAttribute("placeholder", getLabel("Search"));
  }
  
  static public class SearchActionListener extends EventListener<UIQuickSearchForm> {
    public void execute(Event<UIQuickSearchForm> event) throws Exception {
      UIQuickSearchForm uiForm = event.getSource();
      UIFormStringInput formStringInput = uiForm.getUIStringInput(FIELD_SEARCHVALUE);
      String text = formStringInput.getValue();
      if (!ForumUtils.isEmpty(text)) {
        text = CommonUtils.encodeSpecialCharInSearchTerm(text);
        ForumService forumService = (ForumService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ForumService.class);
        UIForumPortlet forumPortlet = uiForm.getAncestorOfType(UIForumPortlet.class);
        UserProfile userProfile = forumPortlet.getUserProfile();
        String type = ForumUtils.EMPTY_STR;
        if (userProfile.getUserRole() == 0)
          type = "true,all";
        else
          type = "false,all";
        List<String> forumIdsOfModerator = new ArrayList<String>();
        if (userProfile.getUserRole() == 1) {
          String[] strings = userProfile.getModerateForums();
          for (int i = 0; i < strings.length; i++) {
            String str = strings[i].substring(strings[i].lastIndexOf(ForumUtils.SLASH) + 1);
            if (str.length() > 0)
              forumIdsOfModerator.add(str);
          }
        }
        List<ForumSearchResult> list = null;
        try {
          list = forumService.getQuickSearch(text, type, ForumUtils.EMPTY_STR, userProfile.getUserId(), forumPortlet.getInvisibleCategories(), new ArrayList<String>(forumPortlet.getInvisibleForums()), forumIdsOfModerator);
        } catch (Exception e) {
          uiForm.log.warn("\nGetting quick search failure:\n " + e.getCause());
          uiForm.warning("UIQuickSearchForm.msg.failure");
          return;
        }
        UICategoryContainer categoryContainer = forumPortlet.getChild(UICategoryContainer.class);
        categoryContainer.updateIsRender(true);
        forumPortlet.updateIsRendered(ForumUtils.CATEGORIES);
        UICategories categories = categoryContainer.getChild(UICategories.class);
        categories.setIsRenderChild(true);
        UIForumListSearch listSearchEvent = categories.getChild(UIForumListSearch.class);
        UIBreadcumbs breadcumbs = forumPortlet.getChild(UIBreadcumbs.class);
        listSearchEvent.setListSearchEvent(text, list, breadcumbs.getLastPath());
        breadcumbs.setUpdataPath(ForumUtils.FIELD_EXOFORUM_LABEL + ForumUtils.SLASH);
        event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
      } else {
        formStringInput.setValue(ForumUtils.EMPTY_STR);
        uiForm.warning("UIQuickSearchForm.msg.checkEmpty");
      }
    }
  }
}
