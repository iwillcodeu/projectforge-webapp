/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2010 Kai Reinhard (k.reinhard@me.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.web.core;

import java.util.Calendar;
import java.util.Date;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.common.DateHolder;
import org.projectforge.common.DatePrecision;
import org.projectforge.registry.Registry;
import org.projectforge.registry.RegistryEntry;
import org.projectforge.task.TaskDO;
import org.projectforge.user.PFUserDO;
import org.projectforge.web.task.TaskSelectPanel;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.AbstractSecuredForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.SingleButtonPanel;

public class SearchForm extends AbstractSecuredForm<SearchData, SearchPage>
{
  private static final long serialVersionUID = 2638309407446431727L;

  protected DatePanel startDatePanel;

  protected DatePanel stopDatePanel;

  private TaskSelectPanel taskSelectPanel;

  SearchData data;

  public SearchForm(final SearchPage parentPage)
  {
    super(parentPage);
    data = new SearchData();
  }

  @Override
  @SuppressWarnings("serial")
  protected void init()
  {
    super.init();
    add(new FeedbackPanel("feedback").setOutputMarkupId(true));
    startDatePanel = new DatePanel("startDate", new PropertyModel<Date>(data, "modifiedStartDate"), DatePanelSettings.get().withCallerPage(
        parentPage).withSelectPeriodMode(true));
    add(startDatePanel);
    stopDatePanel = new DatePanel("stopDate", new PropertyModel<Date>(data, "modifiedStopDate"), DatePanelSettings.get().withCallerPage(
        parentPage).withSelectPeriodMode(true));
    add(stopDatePanel);
    add(new Label("datesAsUTC", new Model<String>() {
      @Override
      public String getObject()
      {
        return WicketUtils.getUTCDates(data.getModifiedStartDate(), data.getModifiedStopDate());
      }
    }));

    final UserSelectPanel userSelectPanel = new UserSelectPanel("modifiedByUser", new PropertyModel<PFUserDO>(data, "modifiedByUser"),
        parentPage, "userId");
    add(userSelectPanel);
    userSelectPanel.init().withAutoSubmit(true);

    taskSelectPanel = new TaskSelectPanel("task", new PropertyModel<TaskDO>(data, "task"), parentPage, "taskId");
    add(taskSelectPanel);
    taskSelectPanel.setEnableLinks(true);
    taskSelectPanel.init();
    taskSelectPanel.setRequired(false);
    {
      // DropDownChoice: time period
      final LabelValueChoiceRenderer<Integer> lastDaysChoiceRenderer = new LabelValueChoiceRenderer<Integer>();
      lastDaysChoiceRenderer.addValue(0, getString("search.today"));
      lastDaysChoiceRenderer.addValue(1, getString("search.lastDay"));
      for (final int days : new int[] { 3, 7, 14, 30, 60, 90}) {
        lastDaysChoiceRenderer.addValue(days, getLocalizedMessage("search.lastDays", days));
      }
      final DropDownChoice<Integer> lastDaysChoice = new DropDownChoice<Integer>("lastDays", new PropertyModel<Integer>(data, "lastDays"),
          lastDaysChoiceRenderer.getValues(), lastDaysChoiceRenderer) {
        @Override
        protected void onSelectionChanged(final Integer newSelection)
        {
          if (newSelection == null) {
            return;
          }
          final DateHolder dh = new DateHolder(new Date(), DatePrecision.MILLISECOND);
          dh.setEndOfDay();
          data.setModifiedStopDate(dh.getDate());
          dh.setBeginOfDay();
          dh.add(Calendar.DAY_OF_YEAR, -newSelection);
          data.setModifiedStartDate(dh.getDate());
          data.setLastDays(-1);
        }

        @Override
        protected boolean wantOnSelectionChangedNotifications()
        {
          return true;
        }
      };
      lastDaysChoice.setNullValid(true);
      lastDaysChoice.setRequired(false);
      add(lastDaysChoice);
    }
    {
      // DropDownChoice: area
      final LabelValueChoiceRenderer<String> areaChoiceRenderer = new LabelValueChoiceRenderer<String>();
      areaChoiceRenderer.addValue("ALL", getString("filter.all"));
      for (final RegistryEntry entry : Registry.instance().getOrderedList()) {
        if (entry.getDao().hasHistoryAccess(false) == true) {
          areaChoiceRenderer.addValue(entry.getId(), getString(entry.getI18nTitleHeading()));
        }
      }
      final DropDownChoice<String> areaChoice = new DropDownChoice<String>("area", new PropertyModel<String>(data, "area"),
          areaChoiceRenderer.getValues(), areaChoiceRenderer) {
        @Override
        protected void onSelectionChanged(final String newSelection)
        {
          parentPage.refresh();
        }

        @Override
        protected boolean wantOnSelectionChangedNotifications()
        {
          return true;
        }
      };
      areaChoice.setNullValid(true);
      areaChoice.setRequired(false);
      add(areaChoice);
    }
    {
      // DropDownChoice pageSize
      final DropDownChoice<Integer> pageSizeChoice = AbstractListForm.getPageSizeDropDownChoice("pageSize", getLocale(),
          new PropertyModel<Integer>(data, "pageSize"), 0, 100);
      add(pageSizeChoice);
    }
    final Button searchButton = new Button("button", new Model<String>(getString("search"))) {
      @Override
      public final void onSubmit()
      {
        parentPage.refresh();
      }
    };
    add(new SingleButtonPanel("search", searchButton));

    final Button resetButton = new Button("button", new Model<String>(getString("reset"))) {
      @Override
      public final void onSubmit()
      {
        parentPage.refresh();
      }
    };
    add(new SingleButtonPanel("reset", resetButton));
  }
}
