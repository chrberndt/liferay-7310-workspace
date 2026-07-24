# Liferay Calendar Web Customizations

## How to use this workspace

1. Copy a valid (developer) license to `configs/common/deploy` 
1. Set up your development bundle: `./gradlew initBundle`
1. Startup Liferay: `./bundles/tomcat/bin/catalina.sh run`
1. Deploy custom modules to Liferay: `./gradlew deploy`
  In your Liferay logs you should see messages like the following: 
    ```
    2026-07-23 07:54:37.455 INFO  [Refresh Thread: Equinox Container: a69a8d2e-9043-47fb-abe6-e68ea0b1f167][BundleStartStopLogger:40] STOPPED com.liferay.calendar.web_4.0.86 [1205]
    2026-07-23 07:54:37.509 INFO  [Refresh Thread: Equinox Container: a69a8d2e-9043-47fb-abe6-e68ea0b1f167][BundleStartStopLogger:37] STARTED com.liferay.calendar.web_4.0.86 [1205]
    2026-07-23 07:54:37.552 INFO  [fileinstall-directory-watcher][BundleStartStopLogger:37] STARTED com.example.custom.language.properties_1.0.0 [1770]
    ```
1. Login to Liferay (test@liferay.com/test)
1. Create a user and add the user to the default site (so that the user can be
  added to calendar bookings).
1. Setup and configure a local SMTP server or set the logger for `com.liferay.petra.mail.MailEngine`
  to `DEBUG`
1. Create a (widget) page and deploy the Calendar Portlet
1. Convince yourself that the calendar settings contain two additional tabs labeled `Update Email` and `Deleted Email`

## calendar-web-fragment

Calendar Web Fragment adds two tabs to Liferay's calendar settings 
screen: `Update Email` and `Deleted Email` which allow for customizing the respective email templates.
The `From Name` and `From Address` is not customizable but controlled globally via the following `portal-ext.properties`:

```
# Custom Calendar Web Fragment properties
custom.calendar.from.address=christian.berndt@liferay.com
custom.calendar.from.name=Christian Berndt
```

## custom-calendar-email-notification-sender

The `CustomEmailNotificationSender` component overrides Liferay's default 
`com.liferay.calendar.internal.notification.EmailNotificationSender`.

If `custom.calendar.from.address` AND `custom.calendar.from.name` are configured in
`portal-ext.properties` it will set the 'From Address' and the 'From Name' for all
`update` and `moved-to-trash` (delete) Notifications.

Please note, that the default behaviour of `invite` and `reminder` notification remain
unaltered. But this behaviour could easily be adjusted by modifying the `CustomEmailNotificationSender`.


## custom-language-properties

Custom Language Properties adds the `Language.properties` required by the `calendar-web-fragment`.