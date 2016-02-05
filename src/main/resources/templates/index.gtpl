yieldUnescaped '<!doctype html>'
html {
  head {
    title "${model.title} | Project Reactor - legacy"

    include template: 'templates/_head.gtpl'
  }
  body(class: "${model.type}") {
    section(class: 'ui inverted masthead segment') {
      div(class: 'ui page grid') {
        div(class: 'column') {
          nav(class: 'ui inverted menu') {
            div(class: 'header item') {
              a(href: '/') {
                yield 'Project Reactor'
              }
            }
            div(class: 'header item') {
                          a(href: 'http://next.projectreactor.io') {
                            yield 'Reactor NEXT'
                          }
            }
            div(class: 'right menu') {
              a(class: 'item', href: '/docs', title: '2.0 Documentation') {
                i(class: 'book icon') {}
              }
              a(class: 'item', href: 'https://github.com/reactor/reactor/', title: 'Github') {
                i(class: 'github icon') {}
              }
              a(class: 'item', href: 'https://github.com/reactor/reactor/issues', title: 'Issue Tracker') {
                i(class: 'bug icon') {}
              }
              a(class: 'item', href: 'http://stackoverflow.com/search?q=%23reactor', title: 'Community Discussion') {
                i(class: 'comments icon') {}
              }
            }
          }

          div(class: 'ui grid information') {
            div(class: 'seven wide column') {
              h1(class: 'ui inverted header') { yield('Project Reactor') }
              h4(class: 'ui inverted header') { yield('Helping running modern and efficient Applications') }
              newLine()
              p 'Reactor is a foundational library for building reactive fast-data applications on the JVM. It is an implementation of the <a href="https://github.com/reactive-streams/reactive-streams" target="__new">Reactive Streams Specification</a>. You can use Reactor to power an application that has a low tolerance for latency and demands extremely high throughput.'
              ul {
                li('<strong>Extremely Fast</strong> &mdash; Reactor is designed to be extraordinarily fast and can sustain throughput rates on the order of 10\'s of millions of operations per second.')
                li('<strong>Succinct but Powerful</strong> &mdash; Reactor has an extremely powerful, yet succinct API for declaring data transformations and functional composition.')
                li('<strong>Mechanically Sympathetic</strong> &mdash; Reactor makes use of the concept of <a href="http://mechanical-sympathy.blogspot.com/" target="__new">Mechanical Sympathy</a> by building on top of the <a href="https://lmax-exchange.github.io/disruptor/" target="__new">Disruptor RingBuffer</a>.')
                li('<strong>Fully Reactive</strong> &mdash; Reactor is designed to be functional and reactive to allow for easy composition of operations.')
              }
            }
            div(class: 'nine wide column') {
              script(src: "https://gist.github.com/jbrisbin/8b060119acc69f5e6dfd.js") {}
            }
          }
        }
      }

    }

    section(class: 'ui main vertical segment') {
      div(class: 'ui centered page grid') {
        div(class: 'fourteen wide column') {
          div(class: 'ui three column center aligned stackable divided grid') {
            div(class: 'column') {
              div(class: 'ui icon header') {
                i(class: 'book icon') {}
              }
              p 'Learn about Reactor through <a href="#">code samples</a>, <a href="/docs">reference documentation</a>, and the <a href="/docs/api">API Javadoc</a>.'
            }
            div(class: 'column') {
              div(class: 'ui icon header') {
                i(class: 'code icon') {}
              }
              p 'Browse the <a href="https://github.com/reactor/reactor/">source code on GitHub</a> or get involved in the project.'
            }
            div(class: 'column') {
              div(class: 'ui icon header') {
                i(class: 'comments icon') {}
              }
              p 'Interact with the community on <a href="https://groups.google.com/forum/#!forum/reactor-framework">Google Groups</a> or <a href="http://stackoverflow.com/search?q=%23reactor">StackOverflow <em>#reactor</em></a>.'
            }
          }
        }
      }
    }

    section(class: 'ui inverted vertical segment') {
      div(class: 'ui very relaxed stackable page grid') {
        div(class: 'row') {
          div(class: 'column') {
            div(class: 'centered aligned ui inverted header') {
              h1('Getting started with Reactor')
            }
          }
        }

        div(class: 'row') {
          div(class: 'ten wide column') {
            p "To use the core components of Reactor like <code>EventBus</code>, <code>Stream</code>, and others, just use the <code>reactor-core</code> artifact. Just replace <code>\${reactorVersion}</code> in the examples below with the version you want to use (the latest is ${model.currentVersion})."

            div(class: 'ui fluid styled accordion') {
              div(class: 'active title inverted') {
                code('build.gradle')
                i(class: 'dropdown icon') {}
              }
              div(class: 'active content build snippet') {
                yieldUnescaped '''<pre><code>repositories {
  maven { url 'http://repo.spring.io/libs-milestone' }
}

dependencies {
  compile "io.projectreactor:reactor-core:${reactorVersion}"
}</code></pre>'''
              }

              div(class: 'title') {
                code('pom.xml')
                i(class: 'dropdown icon') {}
              }
              div(class: 'content build snippet') {
                yieldUnescaped '''<pre><code>&lt;repositories&gt;
  &lt;repository&gt;
    &lt;id&gt;spring-milestone&lt;/id&gt;
    &lt;name&gt;Spring Milestones&lt;/name&gt;
    &lt;url&gt;https://repo.spring.io/libs-milestone&lt;/url&gt;
  &lt;/repository&gt;
&lt;/repositories&gt;

&lt;dependencies&gt;
  &lt;dependency&gt;
    &lt;groupId&gt;io.projectreactor&lt;/groupId&gt;
    &lt;artifactId&gt;reactor-core&lt;/artifactId&gt;
    &lt;version&gt;${reactorVersion}&lt;/version&gt;
  &lt;/dependency&gt;
&lt;/dependencies&gt;</code></pre>'''
              }
            }
          }
          div(class: 'six wide column') {
            a(class: 'twitter-timeline',
                href: 'https://twitter.com/ProjectReactor',
                'data-dnt': 'true',
                'data-widget-id': '357868564667064320') {
              yield 'Tweets by @ProjectReactor'
            }
            script {
              yieldUnescaped '''!function(d,s,id){var js,fjs=d.getElementsByTagName(s)[0],p=/^http:/.test(d.location)?'http':'https';if(!d.getElementById(id)){js=d.createElement(s);js.id=id;js.src=p+"://platform.twitter.com/widgets.js";fjs.parentNode.insertBefore(js,fjs);}}(document,"script","twitter-wjs");'''
            }
          }
        }
      }
    }

    section(class: 'ui vertical segment') {
      div(class: 'ui very relaxed stackable page grid') {
        div(class: 'row') {
          div(class: 'column') {
            div(class: 'centered aligned ui header') {
              h1('Reactor and Reactive Streams')
              div(class: 'ui horizontal divider') {
                i(class: 'fork icon') {}
              }
            }
          }
        }

        div(class: 'row') {
          div(class: 'column') {
            div(class:'ui grid'){
              div(class:'row'){
                yield 'reactive streams members'
              }
            }
          }
        }
      }
    }

    script(src: '/javascripts/projectreactor.js') {}
  }
}