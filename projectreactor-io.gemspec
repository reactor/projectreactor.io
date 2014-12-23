# encoding: utf-8

$LOAD_PATH.unshift(File.expand_path('../lib/', __FILE__))

Gem::Specification.new do |s|
  s.name          = 'projectreactor-io'
  s.version       = '2.0.0'
  s.homepage      = 'http://projectreactor.io/'
  s.summary       = 'Project Reactor'
  s.description   = 'Main website for the Reactor Project'
  s.license       = 'Apache 2.0'

  s.author        = 'Jon Brisbin'
  s.email         = 'jbrisbin@pivotal.io'

  s.files         = Dir['[A-Z]*'] + Dir['lib/**/*'] + [ 'projectreactor-io.gemspec' ]
  s.require_paths = [ 'lib' ]
end
