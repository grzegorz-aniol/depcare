package com.appga.depcaresuplier.web

import com.appga.depcaresuplier.service.DirectoryQueueService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class Management(
	private val directoryQueueService: DirectoryQueueService
) {

	@PostMapping("/api/directories")
	fun addToDirQueue(path: String) {
		directoryQueueService.addFolder(path)
	}

}