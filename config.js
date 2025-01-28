module.exports = async ({ options, resolveVariable }) => {
    const mailFrom = await resolveVariable('env:MAIL_FROM');
    const array = mailFrom.split(',');
    return {
        mailFrom1: array[0],
        mailFrom2: array[1]
    }
}