package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;

public class SL_Reservation extends SimpleCallout {

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    String lastChanged = info.getLastFieldChanged();

    if ("inpmProductId".equals(lastChanged)) {
      final String strProductId = info.getStringParameter("inpmProductId", IsIDFilter.instance);
      if (StringUtils.isNotEmpty(strProductId)) {
        final Product product = OBDal.getInstance().get(Product.class, strProductId);
        info.addResult("inpcUomId", (String) DalUtil.getId(product.getUOM()));
      }
    }
  }

}
