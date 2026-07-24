package com.example.custom.calendar.email.notification.sender;

import com.example.custom.calendar.email.notification.renderer.NotificationTemplateRenderer;
import com.liferay.calendar.constants.CalendarNotificationTemplateConstants;
import com.liferay.calendar.model.CalendarBooking;
import com.liferay.calendar.model.CalendarNotificationTemplate;
import com.liferay.calendar.notification.*;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.PortletProvider;
import com.liferay.portal.kernel.portlet.PortletProviderUtil;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.SubscriptionSender;
import com.liferay.portal.kernel.util.Validator;
import org.osgi.service.component.annotations.Component;

import java.io.File;

/**
 * @author Christian Berndt
 */
@Component(
        immediate = true,
        property = {
            "notification.type=email",
            "service.ranking:Integer=100"
        },
        service = NotificationSender.class
)
public class CustomEmailNotificationSender implements NotificationSender {

    @Override
    public void sendNotification(
            String fromAddress, String fromName,
            NotificationRecipient notificationRecipient,
            NotificationTemplateContext notificationTemplateContext)
            throws NotificationSenderException {

        try {
            CalendarNotificationTemplate calendarNotificationTemplate =
                    notificationTemplateContext.getCalendarNotificationTemplate();

            // Custom logic for handling moved-to-trash (delete) and update notifications
            String customFromAddress = PropsUtil.get("custom.calendar.from.address");
            String customFromName = PropsUtil.get("custom.calendar.from.name");

            if (Validator.isNotNull(customFromAddress) && Validator.isNotNull(customFromName)) {
                if (calendarNotificationTemplate != null) {
                    if ("update".equals(calendarNotificationTemplate.getNotificationTemplateType()) ||
                        "moved-to-trash".equals(calendarNotificationTemplate.getNotificationTemplateType())) {
                        // Use the custom from address and name
                        fromAddress = customFromAddress;
                        fromName = customFromName;
                    }
                } else {
                    // TODO: No template configured - always replace fromAddress and fromName?
                    fromAddress = customFromAddress;
                    fromName = customFromName;
                }
            }

            // Proceed with default flow
            notificationTemplateContext.setFromAddress(
                    NotificationUtil.getTemplatePropertyValue(
                            calendarNotificationTemplate,
                            CalendarNotificationTemplateConstants.PROPERTY_FROM_ADDRESS,
                            fromAddress));
            notificationTemplateContext.setFromName(
                    NotificationUtil.getTemplatePropertyValue(
                            calendarNotificationTemplate,
                            CalendarNotificationTemplateConstants.PROPERTY_FROM_NAME,
                            fromName));

            notificationTemplateContext.setToAddress(
                    notificationRecipient.getEmailAddress());
            notificationTemplateContext.setToName(
                    notificationRecipient.getName());

            _sendNotification(
                    notificationRecipient, notificationTemplateContext);
        }
        catch (Exception exception) {
            _log.error(exception);
            throw new NotificationSenderException(exception);
        }
    }

    private void _sendNotification(
            NotificationRecipient notificationRecipient,
            NotificationTemplateContext notificationTemplateContext)
            throws NotificationSenderException {

        try {
            SubscriptionSender subscriptionSender = new SubscriptionSender();

            subscriptionSender.addFileAttachment(
                    (File)notificationTemplateContext.getAttribute("icsFile"));
            subscriptionSender.addRuntimeSubscribers(
                    notificationRecipient.getEmailAddress(),
                    notificationRecipient.getName());
            subscriptionSender.setClassName(
                    "com.liferay.calendar.service.impl.CalendarBookingLocalServiceImpl");
//                    CalendarBookingLocalServiceImpl.class.getName());
            subscriptionSender.setClassPK(
                    notificationTemplateContext.getCalendarId());
            subscriptionSender.setCompanyId(
                    notificationTemplateContext.getCompanyId());
            subscriptionSender.setContextAttributes(
                    "[$CALENDAR_NAME$]",
                    notificationTemplateContext.getAttribute("calendarName"),
                    "[$COMPANY_ID$]", notificationTemplateContext.getCompanyId(),
                    "[$EVENT_END_DATE$]",
                    notificationTemplateContext.getAttribute("endTime"),
                    "[$EVENT_LOCATION$]",
                    notificationTemplateContext.getAttribute("location"),
                    "[$EVENT_START_DATE$]",
                    notificationTemplateContext.getAttribute("startTime"),
                    "[$EVENT_TITLE$]",
                    notificationTemplateContext.getAttribute("title"),
                    "[$EVENT_URL$]",
                    notificationTemplateContext.getAttribute("url"),
                    "[$INSTANCE_START_TIME$]",
                    notificationTemplateContext.getAttribute("instanceStartTime"),
                    "[$PORTAL_URL$]",
                    notificationTemplateContext.getAttribute("portalURL"),
                    "[$PORTLET_NAME$]",
                    notificationTemplateContext.getAttribute("portletName"),
                    "[$SITE_NAME$]",
                    notificationTemplateContext.getAttribute("siteName"),
                    "[$TO_NAME$]", notificationTemplateContext.getToName());
            subscriptionSender.setContextCreatorUserPrefix("EVENT");
            subscriptionSender.setFrom(
                    notificationTemplateContext.getFromAddress(),
                    notificationTemplateContext.getFromName());
            subscriptionSender.setHtmlFormat(
                    notificationRecipient.isHTMLFormat());
            subscriptionSender.setMailId(
                    "event", notificationTemplateContext.getCalendarId());
            subscriptionSender.setPortletId(
                    PortletProviderUtil.getPortletId(
                            CalendarBooking.class.getName(),
                            PortletProvider.Action.EDIT));
            subscriptionSender.setScopeGroupId(
                    notificationTemplateContext.getGroupId());

            CalendarNotificationTemplate calendarNotificationTemplate =
                    notificationTemplateContext.getCalendarNotificationTemplate();

            if (calendarNotificationTemplate != null) {
                subscriptionSender.setCreatorUserId(
                        calendarNotificationTemplate.getUserId());
                subscriptionSender.setLocalizedBodyMap(
                        LocalizationUtil.getLocalizationMap(
                                calendarNotificationTemplate.getBody()));
                subscriptionSender.setLocalizedSubjectMap(
                        LocalizationUtil.getLocalizationMap(
                                calendarNotificationTemplate.getSubject()));
            }
            else {
                subscriptionSender.setBody(
                        NotificationTemplateRenderer.render(
                                notificationTemplateContext, NotificationField.BODY,
                                NotificationTemplateRenderer.MODE_HTML));
                subscriptionSender.setSubject(
                        NotificationTemplateRenderer.render(
                                notificationTemplateContext, NotificationField.SUBJECT,
                                NotificationTemplateRenderer.MODE_PLAIN));
            }

            subscriptionSender.flushNotificationsAsync();
        }
        catch (Exception exception) {

            _log.error(exception);

            throw new NotificationSenderException(
                    "Unable to send mail message", exception);
        }
    }

    private static final Log _log = LogFactoryUtil.getLog(CustomEmailNotificationSender.class);

}