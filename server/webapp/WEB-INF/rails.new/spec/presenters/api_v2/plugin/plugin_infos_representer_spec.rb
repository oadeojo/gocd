##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################################################################

require 'rails_helper'

describe ApiV2::Plugin::PluginInfosRepresenter do
  it 'renders all plugin_infos with hal representation' do
    plugin_info = PluginInfo.new('plugin_id', 'plugin_name', 'plugin_version', 'plugin_type', 'plugin_display_name', nil, nil)

    actual_json = ApiV2::Plugin::PluginInfosRepresenter.new([plugin_info]).to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :doc)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/plugin_info')
    expect(actual_json).to have_link(:doc).with_url('https://api.gocd.org/#plugin-info')
    actual_json.delete(:_links)
    expect(actual_json.fetch(:_embedded)).to eq({plugin_info: [ApiV2::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)]})
  end
end
