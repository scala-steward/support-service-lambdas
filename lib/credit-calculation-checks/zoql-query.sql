SELECT Invoice.Amount, Invoice.InvoiceDate, Invoice.InvoiceNumber, Subscription.Name, RatePlanCharge.Name, Subscription.ContractAcceptanceDate, Subscription.ContractEffectiveDate, RatePlanCharge.BillCycleType, RatePlanCharge.TriggerEvent, RatePlanCharge.TriggerDate, RatePlanCharge.ProcessedThroughDate, RatePlanCharge.UpToPeriodsType, RatePlanCharge.UpToPeriods, RatePlanCharge.BillingPeriod, RatePlanCharge.SpecificBillingPeriod, RatePlanCharge.EndDateCondition, RatePlanCharge.EffectiveStartDate, Account.BillCycleDay FROM InvoiceItem WHERE ( RatePlan.AmendmentType = 'NewProduct' OR RatePlan.AmendmentType = 'UpdateProduct' OR RatePlan.AmendmentType = null ) AND Invoice.InvoiceDate > '2019-01-01' AND Subscription.AutoRenew = true AND ( RatePlanCharge.Name = 'GW Oct 18 - 1 Year - Domestic' OR RatePlanCharge.Name = 'GW Oct 18 - 3 Month - Domestic'  OR RatePlanCharge.Name = 'GW Oct 18 - Annual - Domestic' OR RatePlanCharge.Name = 'GW Oct 18 - Quarterly - Domestic' OR RatePlanCharge.Name = 'GW Oct 18 - First 6 issues - Domestic' OR RatePlanCharge.Name = 'GW Oct 18 - 1 Year - ROW' OR RatePlanCharge.Name = 'GW Oct 18 - 3 Month - ROW' OR RatePlanCharge.Name = 'GW Oct 18 - Annual - ROW' OR RatePlanCharge.Name = 'GW Oct 18 - Quarterly - ROW' OR RatePlanCharge.Name = 'GW Oct 18 - First 6 issues - ROW' OR RatePlanCharge.Name = 'Zone A 1 Year' OR RatePlanCharge.Name = 'Zone A 12 Issues' OR RatePlanCharge.Name = 'Zone A 2 Years' OR RatePlanCharge.Name = 'Zone A 3 Years' OR RatePlanCharge.Name = 'Zone A 6 Issues' OR RatePlanCharge.Name = 'Zone A 6 Months' OR RatePlanCharge.Name = 'Zone A Annual' OR RatePlanCharge.Name = 'Zone A Quarterly' OR RatePlanCharge.Name = 'Zone B 1 Year' OR RatePlanCharge.Name = 'Zone B 12 Issues' OR RatePlanCharge.Name = 'Zone B 2 Years' OR RatePlanCharge.Name = 'Zone B 3 Years' OR RatePlanCharge.Name = 'Zone B 6 Issues' OR RatePlanCharge.Name = 'Zone B 6 Months' OR RatePlanCharge.Name = 'Zone B Annual' OR RatePlanCharge.Name = 'Zone B Quarterly' OR RatePlanCharge.Name = 'Zone C 1 Year' OR RatePlanCharge.Name = 'Guardian Weekly 12 Issues' OR RatePlanCharge.Name = 'Guardian Weekly 6 Issues' OR RatePlanCharge.Name = 'Zone C 6 Months' OR RatePlanCharge.Name = 'Zone C Annual' OR RatePlanCharge.Name = 'Zone C Quarterly' OR RatePlanCharge.Name = 'Monday' OR RatePlanCharge.Name = 'Tuesday' OR RatePlanCharge.Name = 'Wednesday' OR RatePlanCharge.Name = 'Thursday' OR RatePlanCharge.Name = 'Friday' OR RatePlanCharge.Name = 'Saturday' OR RatePlanCharge.Name = 'Sunday')