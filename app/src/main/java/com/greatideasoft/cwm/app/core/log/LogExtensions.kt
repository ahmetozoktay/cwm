package com.greatideasoft.cwm.app.core.log

import com.greatideasoft.cwm.app.core.CwmApp

fun logWarn(tag: String = "mylogs", message: String) =
	CwmApp.debug.logger.logWarn(tag, message)
fun logWarn(clazz: Class<Any>, message: String) =
	com.greatideasoft.cwm.data.core.log.logWarn("mylogs_${clazz.simpleName}", message)



fun logError(tag: String = "mylogs", message: String) =
	CwmApp.debug.logger.logError(tag, message)
fun logError(clazz: Class<Any>, message: String) =
	logError("mylogs_${clazz.simpleName}", message)



fun logDebug(tag: String = "mylogs", message: String) =
	CwmApp.debug.logger.logDebug(tag, message)
fun logDebug(clazz: Class<Any>, message: String) =
	logDebug("mylogs_${clazz.simpleName}", message)



fun logInfo(tag: String = "mylogs", message: String) =
	CwmApp.debug.logger.logInfo(tag, message)
fun logInfo(clazz: Class<Any>, message: String) =
	logInfo("mylogs_${clazz.simpleName}", message)



fun logWtf(tag: String = "mylogs", message: String) =
	CwmApp.debug.logger.logWtf(tag, message)
fun logWtf(clazz: Class<Any>, message: String) =
	logWtf("mylogs_${clazz.simpleName}", message)
