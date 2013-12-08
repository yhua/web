'use strict'

class Controller
  constructor: ($rootScope, adminNewsService) ->
    @kindergarten =
      id: 1,
      name: 'school23'

    @adminUser =
      id: 1
      name: '豆瓣'

    $rootScope.tabName = 'bulletin'

    @newsletters = adminNewsService.bind(kg: @kindergarten.name, admin_id: @adminUser.id).query(() =>
      for news in @newsletters
        do (news) => news.readCount = 100
    )

    @publish = (news) =>
      news.pushlished = true
      news.$save(kg: @kindergarten.name, news_id: news.id)

    @hidden = (news) =>
      news.pushlished = false
      news.$save(kg: @kindergarten.name, news_id: news.id)


angular.module('admin').controller 'BulletinManageCtrl', ['$rootScope','adminNewsService', Controller]
