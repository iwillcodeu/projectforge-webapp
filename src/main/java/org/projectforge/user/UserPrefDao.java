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
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.projectforge.access.AccessException;
import org.projectforge.access.OperationType;
import org.projectforge.common.NumberHelper;
import org.projectforge.common.StringHelper;
import org.projectforge.core.BaseDO;
import org.projectforge.core.BaseDao;
import org.projectforge.core.BaseSearchFilter;
import org.projectforge.core.DefaultBaseDO;
import org.projectforge.core.QueryFilter;
import org.projectforge.core.UserPrefParameter;
import org.projectforge.database.HibernateUtils;
import org.projectforge.fibu.KundeDO;
import org.projectforge.fibu.KundeDao;
import org.projectforge.fibu.ProjektDO;
import org.projectforge.fibu.ProjektDao;
import org.projectforge.fibu.kost.Kost2DO;
import org.projectforge.fibu.kost.Kost2Dao;
import org.projectforge.task.TaskDO;
import org.projectforge.task.TaskDao;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class UserPrefDao extends BaseDao<UserPrefDO>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UserPrefDao.class);

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[] { "user.username", "user.firstname", "user.lastname"};

  private Kost2Dao kost2Dao;

  private KundeDao kundeDao;

  private ProjektDao projektDao;

  private TaskDao taskDao;

  private UserDao userDao;

  @Override
  protected String[] getAdditionalSearchFields()
  {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  public UserPrefDao()
  {
    super(UserPrefDO.class);
  }

  /**
   * Gets all names of entries of the given area for the current logged in user
   * @param area
   * @return
   */
  public String[] getPrefNames(final UserPrefArea area)
  {
    final PFUserDO user = PFUserContext.getUser();
    @SuppressWarnings("unchecked")
    final List<Object> list = getSession().createQuery("select name from UserPrefDO t where user_fk=? and areaString = ? order by name")
        .setInteger(0, user.getId()).setParameter(1, area.getId()).list();
    final String[] result = new String[list.size()];
    int i = 0;
    for (final Object oa : list) {
      result[i++] = (String) oa;
    }
    return result;
  }

  /**
   * Does (another) entry for the given user with the given area and name already exists?
   * @param id of the current data object (null for new objects).
   * @param user
   * @param area
   * @param name
   * @return
   */
  @SuppressWarnings("unchecked")
  public boolean doesParameterNameAlreadyExist(final Integer id, final PFUserDO user, final UserPrefArea area, final String name)
  {
    Validate.notNull(user);
    Validate.notNull(area);
    Validate.notNull(name);
    final List<UserPrefDO> list;
    if (id != null) {
      list = getHibernateTemplate().find("from UserPrefDO u where pk <> ? and u.user.id = ? and areaString = ? and name = ?",
          new Object[] { id, user.getId(), area.getId(), name});
    } else {
      list = getHibernateTemplate().find("from UserPrefDO u where u.user.id = ? and areaString = ? and name = ?",
          new Object[] { user.getId(), area.getId(), name});
    }
    if (CollectionUtils.isNotEmpty(list) == true) {
      return true;
    }
    return false;
  }

  @Override
  public List<UserPrefDO> getList(final BaseSearchFilter filter)
  {
    final UserPrefFilter myFilter = (UserPrefFilter) filter;
    final QueryFilter queryFilter = new QueryFilter(filter);
    if (myFilter.getArea() != null) {
      queryFilter.add(Restrictions.eq("areaString", myFilter.getArea().getId()));
    }
    queryFilter.addOrder(Order.asc("areaString"));
    queryFilter.addOrder(Order.asc("name"));
    final List<UserPrefDO> list = getList(queryFilter);
    return list;
  }

  public UserPrefDO getUserPref(final UserPrefArea area, final String name)
  {
    final PFUserDO user = PFUserContext.getUser();
    @SuppressWarnings("unchecked")
    final List<UserPrefDO> list = getHibernateTemplate().find("from UserPrefDO u where u.user.id = ? and u.areaString = ? and u.name = ?",
        new Object[] { user.getId(), area.getId(), name});
    if (list == null || list.size() != 1) {
      return null;
    }
    return list.get(0);
  }

  public List<UserPrefDO> getUserPrefs(final UserPrefArea area)
  {
    final PFUserDO user = PFUserContext.getUser();
    @SuppressWarnings("unchecked")
    final List<UserPrefDO> list = getHibernateTemplate().find("from UserPrefDO u where u.user.id = ? and u.areaString = ?",
        new Object[] { user.getId(), area.getId()});
    return list;
  }

  /**
   * Adds the object fields as parameters to the given userPref. Fields without the annotation UserPrefParameter will be ignored.
   * @param userPref
   * @param obj
   * @see #fillFromUserPrefParameters(UserPrefDO, Object)
   */
  public void addUserPrefParameters(final UserPrefDO userPref, final Object obj)
  {
    addUserPrefParameters(userPref, obj.getClass(), obj);
  }

  /**
   * Adds the fields of the bean type represented by the given area as parameters to the given userPref. Fields without the annotation
   * UserPrefParameter will be ignored.
   * @param userPref
   * @param area
   * @see #fillFromUserPrefParameters(UserPrefDO, Object)
   */
  public void addUserPrefParameters(final UserPrefDO userPref, final UserPrefArea area)
  {
    addUserPrefParameters(userPref, area.getBeanType(), null);
  }

  private void addUserPrefParameters(final UserPrefDO userPref, final Class< ? > beanType, final Object obj)
  {
    Validate.notNull(userPref);
    Validate.notNull(beanType);
    final Field[] fields = beanType.getDeclaredFields();
    AccessibleObject.setAccessible(fields, true);
    int no = 0;
    for (final Field field : fields) {
      if (field.isAnnotationPresent(UserPrefParameter.class) == true) {
        final UserPrefEntryDO userPrefEntry = new UserPrefEntryDO();
        userPrefEntry.setParameter(field.getName());
        if (obj != null) {
          Object value = null;
          try {
            value = field.get(obj);
            userPrefEntry.setValue(convertParameterValueToString(value));
          } catch (IllegalAccessException ex) {
            log.error(ex.getMessage(), ex);
          }
          userPrefEntry.valueAsObject = value;
        }
        evaluateAnnotation(userPrefEntry, beanType, field);
        if (userPrefEntry.orderString == null) {
          userPrefEntry.orderString = "ZZZ" + StringHelper.format2DigitNumber(no++);
        }
        userPref.addUserPrefEntry(userPrefEntry);
      }
    }
  }

  private void evaluateAnnotations(final UserPrefDO userPref, final Class< ? > beanType)
  {
    if (userPref.getUserPrefEntries() == null) {
      return;
    }
    final Field[] fields = beanType.getDeclaredFields();
    int no = 0;
    for (final Field field : fields) {
      if (field.isAnnotationPresent(UserPrefParameter.class) == true) {
        UserPrefEntryDO userPrefEntry = null;
        for (final UserPrefEntryDO entry : userPref.getUserPrefEntries()) {
          if (field.getName().equals(entry.getParameter()) == true) {
            userPrefEntry = entry;
            break;
          }
        }
        if (userPrefEntry == null) {
          userPrefEntry = new UserPrefEntryDO();
          evaluateAnnotation(userPrefEntry, beanType, field);
          userPref.addUserPrefEntry(userPrefEntry);
        } else {
          evaluateAnnotation(userPrefEntry, beanType, field);
        }
        if (StringUtils.isBlank(userPrefEntry.orderString) == true) {
          userPrefEntry.orderString = "ZZZ" + StringHelper.format2DigitNumber(no++);
        }
        userPrefEntry.setParameter(field.getName());
      }
    }
  }

  private void evaluateAnnotation(final UserPrefEntryDO userPrefEntry, final Class< ? > beanType, final Field field)
  {
    final UserPrefParameter ann = field.getAnnotation(UserPrefParameter.class);
    userPrefEntry.i18nKey = ann.i18nKey();
    userPrefEntry.tooltipI18nKey = ann.tooltipI18nKey();
    userPrefEntry.dependsOn = StringUtils.isNotBlank(ann.dependsOn()) ? ann.dependsOn() : null;
    userPrefEntry.required = ann.required();
    userPrefEntry.multiline = ann.multiline();
    userPrefEntry.orderString = StringUtils.isNotBlank(ann.orderString()) ? ann.orderString() : null;
    if (String.class.isAssignableFrom(field.getType()) == true) {
      userPrefEntry.maxLength = HibernateUtils.getPropertyLength(beanType, field.getName());
    }
    userPrefEntry.type = field.getType();
  }

  /**
   * Fill object fields from the parameters of the given userPref.
   * @param userPref
   * @param obj
   * @see #addUserPrefParameters(UserPrefDO, Object)
   */
  public void fillFromUserPrefParameters(final UserPrefDO userPref, final Object obj)
  {
    Validate.notNull(userPref);
    Validate.notNull(obj);
    final Field[] fields = obj.getClass().getDeclaredFields();
    AccessibleObject.setAccessible(fields, true);
    if (userPref.getUserPrefEntries() != null) {
      for (final UserPrefEntryDO entry : userPref.getUserPrefEntries()) {
        Field field = null;
        for (final Field f : fields) {
          if (f.getName().equals(entry.getParameter()) == true) {
            field = f;
            break;
          }
        }
        if (field == null) {
          log.error("Declared field '" + entry.getParameter() + "' not found for " + obj.getClass() + ". Ignoring parameter.");
        } else {
          final Object value = getParameterValue(field.getType(), entry.getValue());
          try {
            field.set(obj, value);
          } catch (IllegalArgumentException ex) {
            log.error(ex.getMessage()
                + " While setting declared field '"
                + entry.getParameter()
                + "' of "
                + obj.getClass()
                + ". Ignoring parameter.", ex);
          } catch (IllegalAccessException ex) {
            log.error(ex.getMessage()
                + " While setting declared field '"
                + entry.getParameter()
                + "' of "
                + obj.getClass()
                + ". Ignoring parameter.", ex);
          }
        }
      }
    }
  }

  public void setValueObject(final UserPrefEntryDO userPrefEntry, final Object value)
  {
    userPrefEntry.setValue(convertParameterValueToString(value));
    updateParameterValueObject(userPrefEntry);
  }

  /**
   * @param value
   * @return
   * @see #getParameterValue(String)
   */
  public String convertParameterValueToString(final Object value)
  {
    if (value == null) {
      return null;
    }
    if (value instanceof BaseDO< ? >) {
      return String.valueOf(((BaseDO< ? >) value).getId());
    }
    return String.valueOf(value);
  }

  /**
   * Sets the value object by converting it from the value string. The type of the userPrefEntry must be given.
   * @param userPrefEntry
   */
  public void updateParameterValueObject(final UserPrefEntryDO userPrefEntry)
  {
    userPrefEntry.valueAsObject = getParameterValue(userPrefEntry.getType(), userPrefEntry.getValue());
  }

  /**
   * @param value
   * @return
   * @see #convertParameterValueToString(Object)
   */
  @SuppressWarnings("unchecked")
  public Object getParameterValue(final Class< ? > type, final String str)
  {
    if (str == null) {
      return null;
    }
    if (type.isAssignableFrom(String.class) == true) {
      return str;
    } else if (type.isAssignableFrom(Integer.class) == true) {
      return Integer.valueOf(str);
    } else if (DefaultBaseDO.class.isAssignableFrom(type) == true) {
      final Integer id = NumberHelper.parseInteger(str);
      if (id != null) {
        if (PFUserDO.class.isAssignableFrom(type) == true) {
          return userDao.getOrLoad(id);
        } else if (TaskDO.class.isAssignableFrom(type) == true) {
          return taskDao.getOrLoad(id);
        } else if (Kost2DO.class.isAssignableFrom(type) == true) {
          return kost2Dao.getOrLoad(id);
        } else if (ProjektDO.class.isAssignableFrom(type) == true) {
          return projektDao.getOrLoad(id);
        } else {
          log.warn("getParameterValue: Type '" + type + "' not supported. May-be it does not work.");
          return getHibernateTemplate().load(type, id);
        }
      } else {
        return null;
      }
    } else if (KundeDO.class.isAssignableFrom(type) == true) {
      final Integer id = NumberHelper.parseInteger(str);
      if (id != null) {
        return kundeDao.getOrLoad(id);
      } else {
        return null;
      }
    } else if (type.isEnum() == true) {
      return Enum.valueOf((Class<Enum>) type, str);
    }
    log.error("UserPrefDao does not yet support parameters from type: " + type);
    return null;
  }

  @Override
  public UserPrefDO internalGetById(Serializable id)
  {
    final UserPrefDO userPref = super.internalGetById(id);
    if (userPref == null) {
      return null;
    }
    if (userPref.getArea() != null) {
      evaluateAnnotations(userPref, userPref.getArea().getBeanType());
    }
    return userPref;
  }

  /**
   * @return Always true, no generic select access needed for user pref objects.
   * @see org.projectforge.core.BaseDao#hasSelectAccess()
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final boolean throwException)
  {
    return true;
  }

  /**
   * @see org.projectforge.core.BaseDao#hasAccess(Object, OperationType)
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final UserPrefDO obj, final UserPrefDO oldObj, final OperationType operationType,
      final boolean throwException)
  {
    if (accessChecker.userEquals(user, obj.getUser()) == true) {
      return true;
    }
    if (throwException == true) {
      throw new AccessException("user.pref.error.userIsNotOwner");
    } else {
      return false;
    }
  }

  @Override
  public UserPrefDO newInstance()
  {
    return new UserPrefDO();
  }

  public void setKost2Dao(Kost2Dao kost2Dao)
  {
    this.kost2Dao = kost2Dao;
  }

  public void setKundeDao(KundeDao kundeDao)
  {
    this.kundeDao = kundeDao;
  }

  public void setProjektDao(ProjektDao projektDao)
  {
    this.projektDao = projektDao;
  }

  public void setTaskDao(TaskDao taskDao)
  {
    this.taskDao = taskDao;
  }

  public void setUserDao(UserDao userDao)
  {
    this.userDao = userDao;
  }
}
