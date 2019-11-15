/*
 * Copyright 2017-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.rules.coercer;

import com.facebook.buck.core.cell.CellPathResolver;
import com.facebook.buck.core.model.BuildTarget;
import com.facebook.buck.core.model.TargetConfiguration;
import com.facebook.buck.core.path.ForwardRelativePath;
import com.facebook.buck.io.filesystem.ProjectFilesystem;
import com.facebook.buck.rules.macros.LocationMacro;
import com.facebook.buck.util.types.Pair;
import com.google.common.collect.ImmutableList;
import java.util.Optional;

/** Coerces `$(location ...)` macros into {@link LocationMacro}. */
class LocationMacroTypeCoercer extends AbstractLocationMacroTypeCoercer<LocationMacro> {

  public LocationMacroTypeCoercer(TypeCoercer<BuildTarget> buildTargetTypeCoercer) {
    super(buildTargetTypeCoercer);
  }

  @Override
  public Class<LocationMacro> getOutputClass() {
    return LocationMacro.class;
  }

  @Override
  public LocationMacro coerce(
      CellPathResolver cellRoots,
      ProjectFilesystem filesystem,
      ForwardRelativePath pathRelativeToProjectRoot,
      TargetConfiguration targetConfiguration,
      ImmutableList<String> args)
      throws CoerceFailedException {
    if (args.size() != 1 || args.get(0).isEmpty()) {
      throw new CoerceFailedException(
          String.format("expected exactly one argument (found %d)", args.size()));
    }
    Pair<BuildTarget, Optional<String>> target =
        coerceTarget(
            cellRoots, filesystem, pathRelativeToProjectRoot, targetConfiguration, args.get(0));
    return LocationMacro.of(target.getFirst(), target.getSecond());
  }
}
