(ns rumahsewa141.routes.member
  (:require [rumahsewa141.layout :as layout]
            [rumahsewa141.services.user :as user]
            [rumahsewa141.services.transaction :as transaction]
            [rumahsewa141.views :as views]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as response]
            [ring.util.response :refer [redirect]]
            [buddy.hashers :as hashers]))

(defn do-update-user
  "Update user info controller. Render success page afterwards."
  [{{id :id} :identity {:keys [nickname phone_no]} :params}]
  (when-let [_ (user/update-user-info id nickname phone_no)]
    (layout/render "success.html" {:title "Done!"
                                   :description "You have updated your info."})))

(defn- update-password
  "Update user password with new password. Render success page
  afterwards."
  [id new]
  (when-let [_ (user/change-user-password id new)]
    (layout/render "success.html" {:title "Success!"
                                   :description "Password changed."})))

(defn change-password
  "Change password controller.

  If password confirmation, render error page saying so.
  If password is wrong, render error page saying so.
  Else, update password with new password."
  [{{:keys [id username]} :identity {:keys [old new confirm]} :params}]
  (cond
    (not= new confirm) (layout/render "error_message.html" {:description "Wrong password confirmation."})
    (user/wrong-password? username old) (layout/render "error_message.html" {:description "Wrong password."})
    :else (update-password id new)))

(defn member-page
  "Render member page with the supplied section. The get-content-fn
  will supply content required for that section. The subsection is
  optional and only used in settings section."
  [section get-content-fn {{:keys [username admin]} :identity} & [subsection]]
  (if (true? admin)
    (redirect "/admin")
    (layout/render "member.html" (merge {:username username
                                         :section section
                                         :subsection subsection}
                                        (if (nil? get-content-fn)
                                          nil
                                          (get-content-fn))))))

(defn settings-page
  "Render settings page for member."
  [subsection req & [get-content-fn]]
  (member-page "settings" get-content-fn req subsection))

(defroutes member-routes
  (GET "/member" req (member-page "overview" (transaction/user-bills req) req))
  (GET ["/member/history/:page" :page #"[1-9][0-9]*"] [page :as req]
       (member-page "history"
                    (views/history-view (Long/parseLong page) (transaction/transactions-count req) (transaction/latest-transactions req))
                    req))
  (GET "/member/settings/profile" req (settings-page "profile" req (user/user-info req)))
  (GET "/member/settings/account" req (settings-page "account" req))
  (POST "/settings/profile" req (do-update-user req))
  (POST "/settings/account" req (change-password req)))
