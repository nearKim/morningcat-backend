<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MorningCat Daily Briefing - ${briefing.date}</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            line-height: 1.6;
            color: #333;
            background-color: #f5f5f5;
            margin: 0;
            padding: 0;
        }
        .container {
            max-width: 600px;
            margin: 0 auto;
            background-color: #ffffff;
            box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px 20px;
            text-align: center;
        }
        .header h1 {
            margin: 0;
            font-size: 28px;
            font-weight: 300;
        }
        .header .date {
            font-size: 16px;
            opacity: 0.9;
            margin-top: 5px;
        }
        .content {
            padding: 20px;
        }
        .greeting {
            font-size: 20px;
            color: #667eea;
            margin-bottom: 20px;
        }
        .section {
            margin-bottom: 30px;
            border-bottom: 1px solid #eee;
            padding-bottom: 20px;
        }
        .section:last-child {
            border-bottom: none;
        }
        .section-title {
            font-size: 20px;
            color: #764ba2;
            margin-bottom: 15px;
            display: flex;
            align-items: center;
        }
        .section-icon {
            width: 24px;
            height: 24px;
            margin-right: 10px;
        }
        .weather-card {
            background: linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%);
            border-radius: 10px;
            padding: 20px;
            color: #1a1a1a;
        }
        .weather-main {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 15px;
        }
        .weather-temp {
            font-size: 36px;
            font-weight: bold;
        }
        .weather-condition {
            font-size: 18px;
        }
        .weather-details {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 10px;
            font-size: 14px;
        }
        .news-item {
            margin-bottom: 15px;
            padding: 15px;
            background-color: #f8f9fa;
            border-radius: 8px;
        }
        .news-headline {
            font-size: 16px;
            font-weight: bold;
            color: #333;
            margin-bottom: 5px;
        }
        .news-summary {
            font-size: 14px;
            color: #666;
            margin-bottom: 5px;
        }
        .news-link {
            font-size: 12px;
            color: #667eea;
            text-decoration: none;
        }
        .financial-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 10px;
            background-color: #f8f9fa;
            border-radius: 8px;
            margin-bottom: 10px;
        }
        .financial-symbol {
            font-weight: bold;
            font-size: 16px;
        }
        .financial-name {
            font-size: 14px;
            color: #666;
        }
        .financial-price {
            font-size: 18px;
            font-weight: bold;
        }
        .financial-change {
            font-size: 14px;
            padding: 2px 8px;
            border-radius: 4px;
        }
        .positive {
            color: #28a745;
            background-color: #d4edda;
        }
        .negative {
            color: #dc3545;
            background-color: #f8d7da;
        }
        .calendar-event {
            padding: 15px;
            background-color: #f8f9fa;
            border-radius: 8px;
            margin-bottom: 10px;
        }
        .event-title {
            font-weight: bold;
            color: #333;
            margin-bottom: 5px;
        }
        .event-time {
            color: #667eea;
            font-size: 14px;
        }
        .event-location {
            color: #666;
            font-size: 14px;
        }
        .tip-card {
            background-color: #fff3cd;
            border-left: 4px solid #ffc107;
            padding: 15px;
            margin-bottom: 10px;
        }
        .tip-category {
            font-weight: bold;
            color: #856404;
            margin-bottom: 5px;
        }
        .tip-content {
            color: #856404;
        }
        .entertainment-card {
            padding: 15px;
            background-color: #f8f9fa;
            border-radius: 8px;
            margin-bottom: 10px;
        }
        .entertainment-title {
            font-weight: bold;
            color: #333;
            margin-bottom: 5px;
        }
        .entertainment-type {
            display: inline-block;
            font-size: 12px;
            background-color: #667eea;
            color: white;
            padding: 2px 8px;
            border-radius: 4px;
            margin-bottom: 5px;
        }
        .entertainment-rating {
            color: #ffc107;
            font-weight: bold;
        }
        .footer {
            background-color: #f8f9fa;
            padding: 20px;
            text-align: center;
            font-size: 12px;
            color: #666;
        }
        .weekend-badge {
            display: inline-block;
            background-color: #28a745;
            color: white;
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 12px;
            margin-left: 10px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>MorningCat Daily Briefing</h1>
            <div class="date">${briefing.dayOfWeek}, ${briefing.date}</div>
            <div class="location">${briefing.location}</div>
            <#if briefing.isWeekend>
                <span class="weekend-badge">Weekend Edition</span>
            </#if>
        </div>
        
        <div class="content">
            <div class="greeting">Good morning, ${user.name}! 🌅</div>
            
            <#if weather??>
            <div class="section">
                <h2 class="section-title">
                    <span class="section-icon">☀️</span>
                    Weather
                </h2>
                <div class="weather-card">
                    <div class="weather-main">
                        <div>
                            <div class="weather-temp">${weather.currentTemp?string("0.0")}°C</div>
                            <div class="weather-condition">${weather.condition}</div>
                        </div>
                        <div>
                            <div>↑ ${weather.maxTemp?string("0.0")}°C</div>
                            <div>↓ ${weather.minTemp?string("0.0")}°C</div>
                        </div>
                    </div>
                    <div class="weather-details">
                        <div>💧 Humidity: ${weather.humidity}%</div>
                        <div>☀️ UV Index: ${weather.uvIndex}</div>
                        <div>🌧️ Precipitation: ${weather.precipitation}%</div>
                    </div>
                </div>
            </div>
            </#if>
            
            <#if news?has_content>
            <div class="section">
                <h2 class="section-title">
                    <span class="section-icon">📰</span>
                    News Headlines
                </h2>
                <#list news as article>
                    <div class="news-item">
                        <div class="news-headline">${article.headline}</div>
                        <div class="news-summary">${article.summary}</div>
                        <a href="${article.url}" class="news-link">Read more →</a>
                    </div>
                </#list>
            </div>
            </#if>
            
            <#if financialQuotes?has_content>
            <div class="section">
                <h2 class="section-title">
                    <span class="section-icon">📈</span>
                    Financial Markets
                </h2>
                <#list financialQuotes as quote>
                    <div class="financial-item">
                        <div>
                            <div class="financial-symbol">${quote.symbol}</div>
                            <div class="financial-name">${quote.name}</div>
                        </div>
                        <div>
                            <div class="financial-price">$${quote.price?string("0.00")}</div>
                            <div class="financial-change ${quote.isPositive?then('positive', 'negative')}">
                                ${quote.isPositive?then('+', '')}${quote.changePercent?string("0.00")}%
                            </div>
                        </div>
                    </div>
                </#list>
            </div>
            </#if>
            
            <#if calendarEvents?has_content>
            <div class="section">
                <h2 class="section-title">
                    <span class="section-icon">📅</span>
                    Today's Calendar
                </h2>
                <#list calendarEvents as event>
                    <div class="calendar-event">
                        <div class="event-title">${event.title}</div>
                        <div class="event-time">🕐 ${event.startTime} - ${event.endTime}</div>
                        <#if event.location?has_content>
                            <div class="event-location">📍 ${event.location}</div>
                        </#if>
                        <#if event.description?has_content>
                            <div>${event.description}</div>
                        </#if>
                    </div>
                </#list>
            </div>
            </#if>
            
            <#if selfImprovementTips?has_content>
            <div class="section">
                <h2 class="section-title">
                    <span class="section-icon">💡</span>
                    Daily Tips
                </h2>
                <#list selfImprovementTips as tip>
                    <div class="tip-card">
                        <div class="tip-category">${tip.category}</div>
                        <div class="tip-content">${tip.tip}</div>
                        <#if tip.source?has_content>
                            <div style="font-size: 12px; margin-top: 5px;">— ${tip.source}</div>
                        </#if>
                    </div>
                </#list>
            </div>
            </#if>
            
            <#if entertainmentRecommendations?has_content>
            <div class="section">
                <h2 class="section-title">
                    <span class="section-icon">🎬</span>
                    Entertainment Recommendations
                </h2>
                <#list entertainmentRecommendations as rec>
                    <div class="entertainment-card">
                        <div class="entertainment-title">${rec.title}</div>
                        <span class="entertainment-type">${rec.type}</span>
                        <#if rec.rating??>
                            <span class="entertainment-rating">★ ${rec.rating}</span>
                        </#if>
                        <div style="margin-top: 5px;">${rec.description}</div>
                    </div>
                </#list>
            </div>
            </#if>
        </div>
        
        <div class="footer">
            <p>Have a productive day!</p>
            <p>© 2025 MorningCat. All rights reserved.</p>
            <p>
                <a href="https://morningcat.app/preferences" style="color: #667eea;">Update Preferences</a> |
                <a href="https://morningcat.app/unsubscribe" style="color: #667eea;">Unsubscribe</a>
            </p>
        </div>
    </div>
</body>
</html>