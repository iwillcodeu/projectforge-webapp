/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2011 Kai Reinhard (k.reinhard@me.com)
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

package org.projectforge.user;

import java.io.Serializable;

import org.apache.commons.lang.Validate;
import org.projectforge.fibu.KundeFavorite;
import org.projectforge.fibu.ProjektFavorite;
import org.projectforge.jira.JiraProject;
import org.projectforge.task.TaskFavorite;
import org.projectforge.timesheet.TimesheetDO;

/**
 * User preferences are supported by different areas. These areas are defined inside this enum.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class UserPrefArea implements Serializable
{
  private static final long serialVersionUID = -6594785391128587090L;

  static final int MAX_ID_LENGTH = 20;

  public static final UserPrefArea KUNDE_FAVORITE = new UserPrefArea("KUNDE_FAVORITE", KundeFavorite.class, "kunde.favorite");

  public static final UserPrefArea PROJEKT_FAVORITE = new UserPrefArea("PROJEKT_FAVORITE", ProjektFavorite.class, "projekt.favorite");

  public static final UserPrefArea TASK_FAVORITE = new UserPrefArea("TASK_FAVORITE", TaskFavorite.class, "task.favorite");

  public static final UserPrefArea TIMESHEET_TEMPLATE = new UserPrefArea("TIMESHEET_TEMPLATE", TimesheetDO.class, "timesheet.template");

  public static final UserPrefArea USER_FAVORITE = new UserPrefArea("USER_FAVORITE", UserFavorite.class, "user.favorite");

  public static final UserPrefArea JIRA_PROJECT = new UserPrefArea("JIRA_PROJECT", JiraProject.class, "jira.project");

  private String id;

  private String key;

  private Class< ? > beanType;

  /**
   * The id is used as identity in the data-base.
   */
  public String getId()
  {
    return id;
  }

  /**
   * The key will be used e. g. for i18n (only the suffix not the base i18n key).
   * @return
   */
  public String getKey()
  {
    return key;
  }

  /**
   * Get the whole i18n key.
   * @return
   */
  public String getI18nKey()
  {
    return "user.pref.area." + key;
  }

  /**
   * The type corresponding to this UserPrefArea. This is the bean for which the annotated fields are stored as UserPrefParameterDO's.
   * @return
   */
  public Class< ? > getBeanType()
  {
    return beanType;
  }

  /**
   * @param id Used as identity in the data-base (max-length = 20). Please don't change this id later, otherwise (de)-serialization will
   *          fail (could not read data-base entries).
   * @param clazz The class which contains the user pref parameters.
   * @param key The i18n suffix (i18nkey starts with 'user.pref.area.").
   */
  public UserPrefArea(final String id, final Class< ? > clazz, final String key)
  {
    Validate.isTrue(id.length() <= MAX_ID_LENGTH);
    this.id = id;
    this.beanType = clazz;
    this.key = key;
  }

  public boolean isIn(final UserPrefArea... userPrefAreas)
  {
    for (final UserPrefArea area : userPrefAreas) {
      if (this == area) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString()
  {
    return String.valueOf(id);
  }
}
