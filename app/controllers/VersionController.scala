package controllers

import io.swagger.annotations.{ Api, ApiOperation, ApiResponse, ApiResponses }
import javax.inject.Singleton
import play.api.mvc.{ Action, AnyContent, Controller }

/**
 * VersionController
 * ----------------
 * Author: haqa
 * Date: 02 February 2018 - 10:25
 * Copyright (c) 2017  Office for National Statistics
 */

/**
 * version listings is defined using the BuildInfo feature
 */

@Api("Utils")
@Singleton
class VersionController extends Controller {

  // public api
  @ApiOperation(
    value = "Version List",
    notes = "Provides a full listing of all versions of software related tools - this can be found in the build file.",
    httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Success - Displays a version list as json.")))
  def version: Action[AnyContent] = Action {
    Ok(BuildInfo.toJson).as(JSON)
  }
}