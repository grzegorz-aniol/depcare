package com.appga.depcare.config

import com.appga.depcare.service.repo.RepoDirsProcessor
import javax.enterprise.context.Dependent
import javax.inject.Inject
import javax.inject.Named

@Dependent
@Named("ConcurrencyConfig")
class ConcurrencyConfig(
	@Inject
	val processor1: RepoDirsProcessor,
	@Inject
	val processor2: RepoDirsProcessor,
)