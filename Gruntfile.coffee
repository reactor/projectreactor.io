module.exports = (grunt) ->
  grunt.initConfig {
    shell: {
      build: {
        options: {async: true},
        command: './gradlew build'
      }
    },

    sass: {
      options: {
        sourcemap: 'none'
      },

      projectreactor: {
        files: {
          'src/main/resources/public/stylesheets/projectreactor.css': 'src/main/sass/projectreactor.scss'
        }
      }
    },

    coffee: {
      options: {
        separator: ';',
        sourcemap: false
      },

      projectreactor: {
        files: {
          'src/main/resources/public/javascripts/projectreactor.js': 'src/main/coffee/projectreactor.coffee'
        }
      }
    }

    watch: {
      options: {
        livereload: true
      },

      sass: {
        files: ['src/main/sass/**'],

        tasks: ['sass:projectreactor']
      },

      coffee: {
        files: ['src/main/coffee/**'],

        tasks: ['coffee:projectreactor']
      }
    }
  }

  grunt.loadNpmTasks 'grunt-shell'
  grunt.loadNpmTasks 'grunt-contrib-sass'
  grunt.loadNpmTasks 'grunt-contrib-coffee'
  grunt.loadNpmTasks 'grunt-contrib-watch'

  grunt.registerTask 'build', ['sass:projectreactor',
                               'coffee:projectreactor',
                               'shell:build']

  grunt.registerTask 'default', ['sass:projectreactor',
                                 'coffee:projectreactor']