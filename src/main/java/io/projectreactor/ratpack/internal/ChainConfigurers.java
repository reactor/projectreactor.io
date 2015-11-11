/*
 * Copyright (c) 2011-2016 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.projectreactor.ratpack.internal;

import com.google.common.collect.ImmutableMap;
import io.projectreactor.ratpack.RatpackServerCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;
import ratpack.func.Action;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.path.PathBinder;
import ratpack.path.PathBinding;
import ratpack.path.internal.DefaultPathBinding;
import ratpack.server.ServerConfig;

import java.util.*;

/**
 * @author Dave Syer
 *
 */
@Component
public class ChainConfigurers implements Action<Chain> {

	@Autowired(required = false)
	private List<Action<Chain>> delegates = Collections.emptyList();

	@Autowired(required = false)
	private List<Handler> handlers = Collections.emptyList();

	@Autowired(required = false)
	private List<RatpackServerCustomizer> customizers = Collections.emptyList();

	@Override
	public void execute(Chain chain) throws Exception {
		List<Action<Chain>> delegates = new ArrayList<Action<Chain>>(this.delegates);
		for (RatpackServerCustomizer customizer : customizers) {
			delegates.addAll(customizer.getHandlers());
		}
		if (handlers.size() == 1 || delegates.isEmpty()) {
			delegates.add(singleHandlerAction());
		}
		delegates.add(staticResourcesAction(chain.getServerConfig()));
		AnnotationAwareOrderComparator.sort(delegates);
		for (Action<Chain> delegate : delegates) {
			if (!(delegate instanceof ChainConfigurers)) {
				delegate.execute(chain);
			}
		}
	}

	private Action<Chain> staticResourcesAction(final ServerConfig config) {
		return chain -> {
			chain.handler(Handlers.path(new RootBinder(),
					Handlers.assets(config, "static", Arrays.asList("index.html"))));
			chain.handler(Handlers.path(new RootBinder(),
					Handlers.assets(config, "public", Arrays.asList("index.html"))));
		};
	}

	protected static class RootBinder implements PathBinder {

		@Override
		public Optional<PathBinding> bind(String path, Optional<PathBinding> parentBinding) {
			return Optional.of(new DefaultPathBinding("/" + path, "", ImmutableMap
					.<String, String> of(), parentBinding));
		}
	}

	private Action<Chain> singleHandlerAction() {
		return chain -> {
			if (handlers.size() == 1) {
				chain.get(handlers.get(0));
			}
			else {
				throw new IllegalStateException(
						"No Action<Chain> defined so expecting a single Handler (found "
								+ handlers.size() + ")");
			}
		};
	}

}
