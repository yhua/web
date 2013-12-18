'use strict'

class Controller
  constructor: ($stateParams, newsService, readService) ->
    @kindergarten =
      id: 1,
      name: '93740362'
    @user =
      id: 1
      name: '豆瓣'
    readService.markRead(parent_id: @user.id, kg: @kindergarten.name, news_id: $stateParams.news)
    @news = newsService.get(kg: @kindergarten.name, news_id: $stateParams.news)


angular.module('kulebaoApp').controller 'NewsCtrl', [ '$stateParams', 'newsService', 'readService', Controller]